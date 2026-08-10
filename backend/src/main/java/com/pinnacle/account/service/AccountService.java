package com.pinnacle.account.service;

import com.pinnacle.account.dto.AccountSummaryResponse;
import com.pinnacle.entity.Account;
import com.pinnacle.entity.Order;
import com.pinnacle.entity.Position;
import com.pinnacle.entity.Ticker;
import com.pinnacle.entity.enums.LedgerEntryType;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.OrderStatus;
import com.pinnacle.entity.enums.PositionStatus;
import com.pinnacle.marketdata.service.PriceCacheService;
import com.pinnacle.oms.service.LedgerService;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.OrderRepository;
import com.pinnacle.repository.PositionRepository;
import com.pinnacle.repository.TickerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The one place that marks open positions to market. This is genuinely new
 * ground, not something the earlier passes already covered — the equity
 * curve in reporting is realized-P&L-only by design (see its README note);
 * this is the live, mark-to-market number the Dashboard/Portfolio need.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final TickerRepository tickerRepository;
    private final OrderRepository orderRepository;
    private final PriceCacheService priceCacheService;
    private final LedgerService ledgerService;
    private final BigDecimal startingBalance;

    public AccountService(
            AccountRepository accountRepository,
            PositionRepository positionRepository,
            TickerRepository tickerRepository,
            OrderRepository orderRepository,
            PriceCacheService priceCacheService,
            LedgerService ledgerService,
            @Value("${pinnacle.demo.starting-balance}") BigDecimal startingBalance
    ) {
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.tickerRepository = tickerRepository;
        this.orderRepository = orderRepository;
        this.priceCacheService = priceCacheService;
        this.ledgerService = ledgerService;
        this.startingBalance = startingBalance;
    }

    @Transactional(readOnly = true)
    public AccountSummaryResponse getSummary(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        List<Position> openPositions = positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(
                account.getId(), PositionStatus.CLOSED
        );

        BigDecimal unrealizedPnl = openPositions.stream()
                .map(this::unrealizedPnlFor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal equity = account.getBalance().add(unrealizedPnl);

        return new AccountSummaryResponse(account.getCurrency(), account.getBalance(), account.getBuyingPower(), unrealizedPnl, equity);
    }

    /**
     * A pragmatic reset, not the fuller "archive, don't delete" version noted
     * as a future backlog item: this administratively closes open positions
     * and cancels pending orders (without generating Trade rows or further
     * ledger postings for them — they're not being "traded out", just wiped),
     * then posts a single DEMO_RESET ledger entry bringing balance and buying
     * power back to the starting balance exactly. Closed trade history is
     * untouched, since nothing here deletes from the trades table.
     */
    @Transactional
    public void resetDemoBalance(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No trading account for user"));

        List<Position> openPositions = positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(
                account.getId(), PositionStatus.CLOSED
        );
        for (Position position : openPositions) {
            position.setRemainingQuantity(BigDecimal.ZERO);
            position.setStatus(PositionStatus.CLOSED);
            position.setClosedAt(Instant.now());
            positionRepository.save(position);
        }

        List<Order> pendingOrders = orderRepository.findByAccountIdAndStatus(account.getId(), OrderStatus.ROUTED);
        for (Order order : pendingOrders) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }

        BigDecimal delta = startingBalance.subtract(account.getBalance());
        if (delta.signum() != 0) {
            ledgerService.postEntry(account, LedgerEntryType.DEMO_RESET, delta, null, "Demo account reset to starting balance");
        }
    }

    private BigDecimal unrealizedPnlFor(Position position) {
        if (position.getRemainingQuantity().signum() <= 0) return BigDecimal.ZERO;

        Ticker ticker = tickerRepository.findById(position.getTickerId()).orElse(null);
        if (ticker == null) return BigDecimal.ZERO;

        return priceCacheService.getLatestPrice(ticker.getSymbol())
                .map(livePrice -> position.getSide() == OrderSide.BUY
                        ? livePrice.subtract(position.getEntryPrice()).multiply(position.getRemainingQuantity())
                        : position.getEntryPrice().subtract(livePrice).multiply(position.getRemainingQuantity()))
                .orElse(BigDecimal.ZERO); // no live tick yet for this symbol — treat as flat rather than guessing
    }
}
