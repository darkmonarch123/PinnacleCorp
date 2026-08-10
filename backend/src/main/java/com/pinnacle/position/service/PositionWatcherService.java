package com.pinnacle.position.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.Position;
import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.PositionStatus;
import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.PositionRepository;
import com.pinnacle.repository.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Every 5 seconds, checks every position that's still open in some form
 * (OPEN / MODIFIED / PARTIAL_CLOSE) against its ticker's live price. If
 * price has crossed the stop-loss or take-profit, the remaining quantity is
 * closed automatically at the current price — no user action required.
 */
@Service
public class PositionWatcherService {

    private static final Logger log = LoggerFactory.getLogger(PositionWatcherService.class);

    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final TickerRepository tickerRepository;
    private final PriceCacheService priceCacheService;
    private final PositionService positionService;

    public PositionWatcherService(
            PositionRepository positionRepository,
            AccountRepository accountRepository,
            TickerRepository tickerRepository,
            PriceCacheService priceCacheService,
            PositionService positionService
    ) {
        this.positionRepository = positionRepository;
        this.accountRepository = accountRepository;
        this.tickerRepository = tickerRepository;
        this.priceCacheService = priceCacheService;
        this.positionService = positionService;
    }

    @Scheduled(fixedRate = 5000)
    public void checkStopLossAndTakeProfit() {
        List<Position> activePositions = positionRepository.findByStatusIn(
                List.of(PositionStatus.OPEN, PositionStatus.MODIFIED, PositionStatus.PARTIAL_CLOSE)
        );

        for (Position position : activePositions) {
            try {
                evaluate(position);
            } catch (Exception e) {
                log.error("Failed evaluating SL/TP for position {}", position.getId(), e);
            }
        }
    }

    private void evaluate(Position position) {
        if (position.getStopLoss() == null && position.getTakeProfit() == null) return;
        if (position.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) return;

        Ticker ticker = tickerRepository.findById(position.getTickerId()).orElse(null);
        if (ticker == null) return;

        Optional<BigDecimal> priceOpt = priceCacheService.getLatestPrice(ticker.getSymbol());
        if (priceOpt.isEmpty()) return;
        BigDecimal price = priceOpt.get();

        boolean hitStopLoss = position.getStopLoss() != null && (
                position.getSide() == OrderSide.BUY
                        ? price.compareTo(position.getStopLoss()) <= 0
                        : price.compareTo(position.getStopLoss()) >= 0
        );
        boolean hitTakeProfit = position.getTakeProfit() != null && (
                position.getSide() == OrderSide.BUY
                        ? price.compareTo(position.getTakeProfit()) >= 0
                        : price.compareTo(position.getTakeProfit()) <= 0
        );

        if (!hitStopLoss && !hitTakeProfit) return;

        Account account = accountRepository.findById(position.getAccountId()).orElse(null);
        if (account == null) return;

        log.info("Auto-closing position {} ({} {} {}): {} triggered at {}",
                position.getId(), position.getSide(), position.getRemainingQuantity(), ticker.getSymbol(),
                hitStopLoss ? "stop-loss" : "take-profit", price);

        positionService.closePosition(account, ticker, position, position.getRemainingQuantity(), price, null);
    }
}
