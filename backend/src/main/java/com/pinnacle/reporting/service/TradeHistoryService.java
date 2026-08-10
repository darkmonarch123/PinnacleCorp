package com.pinnacle.reporting.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.Trade;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.TickerRepository;
import com.pinnacle.repository.TradeRepository;
import com.pinnacle.reporting.dto.EquityCurvePoint;
import com.pinnacle.reporting.dto.TradeResponse;
import com.pinnacle.reporting.dto.TradeStatsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TradeHistoryService {

    private final TradeRepository tradeRepository;
    private final AccountRepository accountRepository;
    private final TickerRepository tickerRepository;
    private final BigDecimal startingBalance;

    public TradeHistoryService(
            TradeRepository tradeRepository,
            AccountRepository accountRepository,
            TickerRepository tickerRepository,
            @Value("${pinnacle.demo.starting-balance}") BigDecimal startingBalance
    ) {
        this.tradeRepository = tradeRepository;
        this.accountRepository = accountRepository;
        this.tickerRepository = tickerRepository;
        this.startingBalance = startingBalance;
    }

    public enum ResultFilter { ALL, WIN, LOSS }

    @Transactional(readOnly = true)
    public List<TradeResponse> listTrades(UUID userId, String symbolFilter, Instant from, Instant to, ResultFilter result) {
        Account account = resolveAccount(userId);
        Map<UUID, String> symbolCache = new HashMap<>();

        return filteredTrades(account.getId(), symbolFilter, from, to, result, symbolCache).stream()
                .map(t -> TradeResponse.from(t, symbolCache.get(t.getTickerId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TradeStatsResponse getStats(UUID userId) {
        Account account = resolveAccount(userId);
        List<Trade> trades = tradeRepository.findByAccountIdOrderByClosedAtDesc(account.getId());

        int total = trades.size();
        List<Trade> winners = trades.stream().filter(t -> t.getRealizedPnl().signum() > 0).toList();
        List<Trade> losers = trades.stream().filter(t -> t.getRealizedPnl().signum() < 0).toList();

        BigDecimal winRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(winners.size()).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        BigDecimal avgWinLossRatio = null;
        if (!winners.isEmpty() && !losers.isEmpty()) {
            BigDecimal avgWin = sum(winners).divide(BigDecimal.valueOf(winners.size()), 4, RoundingMode.HALF_UP);
            BigDecimal avgLossAbs = sum(losers).abs().divide(BigDecimal.valueOf(losers.size()), 4, RoundingMode.HALF_UP);
            if (avgLossAbs.signum() != 0) {
                avgWinLossRatio = avgWin.divide(avgLossAbs, 2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalPnl = sum(trades);
        BigDecimal maxDrawdown = computeMaxDrawdownPercent(trades);

        return new TradeStatsResponse(total, winners.size(), losers.size(), winRate, avgWinLossRatio, maxDrawdown, totalPnl);
    }

    @Transactional(readOnly = true)
    public List<EquityCurvePoint> getEquityCurve(UUID userId) {
        Account account = resolveAccount(userId);
        List<Trade> trades = tradeRepository.findByAccountIdOrderByClosedAtDesc(account.getId());
        List<Trade> chronological = trades.stream()
                .sorted(Comparator.comparing(Trade::getClosedAt))
                .toList();

        List<EquityCurvePoint> curve = new java.util.ArrayList<>();
        curve.add(new EquityCurvePoint(chronological.isEmpty() ? Instant.now() : chronological.get(0).getOpenedAt(), startingBalance));

        BigDecimal running = startingBalance;
        for (Trade t : chronological) {
            running = running.add(t.getRealizedPnl());
            curve.add(new EquityCurvePoint(t.getClosedAt(), running));
        }
        return curve;
    }

    @Transactional(readOnly = true)
    public String exportCsv(UUID userId) {
        Account account = resolveAccount(userId);
        Map<UUID, String> symbolCache = new HashMap<>();
        List<Trade> trades = filteredTrades(account.getId(), null, null, null, ResultFilter.ALL, symbolCache);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        StringBuilder csv = new StringBuilder("symbol,side,quantity,entry_price,exit_price,realized_pnl,opened_at,closed_at\n");

        for (Trade t : trades) {
            csv.append(symbolCache.get(t.getTickerId())).append(',')
               .append(t.getSide()).append(',')
               .append(t.getQuantity()).append(',')
               .append(t.getEntryPrice()).append(',')
               .append(t.getExitPrice()).append(',')
               .append(t.getRealizedPnl()).append(',')
               .append(fmt.format(t.getOpenedAt())).append(',')
               .append(fmt.format(t.getClosedAt())).append('\n');
        }

        return csv.toString();
    }

    private List<Trade> filteredTrades(UUID accountId, String symbolFilter, Instant from, Instant to,
                                        ResultFilter result, Map<UUID, String> symbolCache) {
        return tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId).stream()
                .filter(t -> from == null || !t.getClosedAt().isBefore(from))
                .filter(t -> to == null || !t.getClosedAt().isAfter(to))
                .filter(t -> {
                    String symbol = symbolCache.computeIfAbsent(t.getTickerId(),
                            id -> tickerRepository.findById(id).map(Ticker::getSymbol).orElse("UNKNOWN"));
                    return symbolFilter == null || symbolFilter.isBlank() || symbol.equalsIgnoreCase(symbolFilter);
                })
                .filter(t -> switch (result) {
                    case ALL -> true;
                    case WIN -> t.getRealizedPnl().signum() > 0;
                    case LOSS -> t.getRealizedPnl().signum() < 0;
                })
                .toList();
    }

    private BigDecimal computeMaxDrawdownPercent(List<Trade> trades) {
        List<Trade> chronological = trades.stream().sorted(Comparator.comparing(Trade::getClosedAt)).toList();

        BigDecimal running = startingBalance;
        BigDecimal peak = startingBalance;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (Trade t : chronological) {
            running = running.add(t.getRealizedPnl());
            if (running.compareTo(peak) > 0) {
                peak = running;
            }
            if (peak.signum() > 0) {
                BigDecimal drawdown = peak.subtract(running).divide(peak, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }
        return maxDrawdown.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<Trade> trades) {
        return trades.stream().map(Trade::getRealizedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Account resolveAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));
    }
}
