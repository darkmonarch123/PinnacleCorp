package com.pinnacle.account.service;

import com.pinnacle.account.dto.AccountSummaryResponse;
import com.pinnacle.entity.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private TickerRepository tickerRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PriceCacheService priceCacheService;
    @Mock private LedgerService ledgerService;

    private AccountService accountService;
    private UUID userId;
    private Account account;
    private Ticker ticker;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, positionRepository, tickerRepository,
                orderRepository, priceCacheService, ledgerService, new BigDecimal("10000.00"));

        userId = UUID.randomUUID();
        account = new Account();
        account.setId(UUID.randomUUID());
        account.setUserId(userId);
        account.setBalance(new BigDecimal("9000.00"));
        account.setBuyingPower(new BigDecimal("7000.00"));

        ticker = new Ticker();
        ticker.setId(UUID.randomUUID());
        ticker.setSymbol("AAPL");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
    }

    private Position openLong(BigDecimal remainingQty, BigDecimal entryPrice) {
        Position p = new Position();
        p.setId(UUID.randomUUID());
        p.setTickerId(ticker.getId());
        p.setSide(OrderSide.BUY);
        p.setStatus(PositionStatus.OPEN);
        p.setRemainingQuantity(remainingQty);
        p.setEntryPrice(entryPrice);
        p.setOpenedAt(Instant.now());
        return p;
    }

    @Test
    @DisplayName("equity marks open positions to market using the live cached price")
    void equityMarksPositionsToMarket() {
        Position position = openLong(new BigDecimal("10"), new BigDecimal("200.00"));
        when(positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(account.getId(), PositionStatus.CLOSED))
                .thenReturn(List.of(position));
        when(tickerRepository.findById(ticker.getId())).thenReturn(Optional.of(ticker));
        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("210.00")));

        AccountSummaryResponse summary = accountService.getSummary(userId);

        // unrealized = (210-200)*10 = 100
        assertThat(summary.unrealizedPnl()).isEqualByComparingTo("100.00");
        assertThat(summary.equity()).isEqualByComparingTo("9100.00"); // balance 9000 + 100
    }

    @Test
    @DisplayName("a position with no live tick yet contributes zero unrealized P&L instead of guessing")
    void noLiveTickContributesZero() {
        Position position = openLong(new BigDecimal("10"), new BigDecimal("200.00"));
        when(positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(account.getId(), PositionStatus.CLOSED))
                .thenReturn(List.of(position));
        when(tickerRepository.findById(ticker.getId())).thenReturn(Optional.of(ticker));
        when(priceCacheService.getLatestPrice("AAPL")).thenReturn(Optional.empty());

        AccountSummaryResponse summary = accountService.getSummary(userId);

        assertThat(summary.unrealizedPnl()).isEqualByComparingTo("0");
        assertThat(summary.equity()).isEqualByComparingTo(account.getBalance());
    }

    @Test
    @DisplayName("reset posts a DEMO_RESET entry that brings balance back to exactly the starting balance")
    void resetPostsCorrectDelta() {
        when(positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(account.getId(), PositionStatus.CLOSED))
                .thenReturn(List.of());
        when(orderRepository.findByAccountIdAndStatus(account.getId(), OrderStatus.ROUTED)).thenReturn(List.of());

        accountService.resetDemoBalance(userId);

        // starting balance 10000 - current balance 9000 = +1000 delta
        verify(ledgerService).postEntry(eq(account), eq(LedgerEntryType.DEMO_RESET), eq(new BigDecimal("1000.00")), isNull(), anyString());
    }

    @Test
    @DisplayName("reset administratively closes every open position without generating a Trade")
    void resetClosesOpenPositions() {
        Position position = openLong(new BigDecimal("10"), new BigDecimal("200.00"));
        when(positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(account.getId(), PositionStatus.CLOSED))
                .thenReturn(List.of(position));
        when(orderRepository.findByAccountIdAndStatus(account.getId(), OrderStatus.ROUTED)).thenReturn(List.of());

        accountService.resetDemoBalance(userId);

        assertThat(position.getStatus()).isEqualTo(PositionStatus.CLOSED);
        assertThat(position.getRemainingQuantity()).isEqualByComparingTo("0");
        verify(positionRepository).save(position);
    }

    @Test
    @DisplayName("reset cancels every pending (ROUTED) order")
    void resetCancelsPendingOrders() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.ROUTED);

        when(positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(account.getId(), PositionStatus.CLOSED))
                .thenReturn(List.of());
        when(orderRepository.findByAccountIdAndStatus(account.getId(), OrderStatus.ROUTED)).thenReturn(List.of(order));

        accountService.resetDemoBalance(userId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("resetting an account already at the starting balance posts no ledger entry")
    void resetAtStartingBalanceIsNoOp() {
        account.setBalance(new BigDecimal("10000.00"));
        when(positionRepository.findByAccountIdAndStatusNotOrderByOpenedAtDesc(account.getId(), PositionStatus.CLOSED))
                .thenReturn(List.of());
        when(orderRepository.findByAccountIdAndStatus(account.getId(), OrderStatus.ROUTED)).thenReturn(List.of());

        accountService.resetDemoBalance(userId);

        verifyNoInteractions(ledgerService);
    }
}
