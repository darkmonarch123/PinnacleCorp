package com.pinnacle.position.service;

import com.pinnacle.entity.*;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.OrderStatus;
import com.pinnacle.entity.enums.OrderType;
import com.pinnacle.entity.enums.PositionStatus;
import com.pinnacle.oms.service.LedgerService;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.PositionRepository;
import com.pinnacle.repository.TickerRepository;
import com.pinnacle.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock private PositionRepository positionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TickerRepository tickerRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private LedgerService ledgerService;

    private PositionService positionService;
    private Account account;
    private Ticker ticker;

    @BeforeEach
    void setUp() {
        positionService = new PositionService(positionRepository, accountRepository, tickerRepository, tradeRepository, ledgerService);

        account = new Account();
        account.setId(UUID.randomUUID());
        account.setBalance(new BigDecimal("10000.00"));
        account.setBuyingPower(new BigDecimal("10000.00"));

        ticker = new Ticker();
        ticker.setId(UUID.randomUUID());
        ticker.setSymbol("AAPL");
    }

    private Position longPosition(BigDecimal quantity, BigDecimal entryPrice) {
        Position p = new Position();
        p.setId(UUID.randomUUID());
        p.setAccountId(account.getId());
        p.setTickerId(ticker.getId());
        p.setSide(OrderSide.BUY);
        p.setStatus(PositionStatus.OPEN);
        p.setQuantity(quantity);
        p.setRemainingQuantity(quantity);
        p.setEntryPrice(entryPrice);
        p.setOpenedAt(Instant.now());
        return p;
    }

    private Position shortPosition(BigDecimal quantity, BigDecimal entryPrice) {
        Position p = longPosition(quantity, entryPrice);
        p.setSide(OrderSide.SELL);
        return p;
    }

    @Test
    @DisplayName("closing a long position for a gain computes (exit - entry) * quantity as realized P&L")
    void closingLongPositionForGain() {
        Position position = longPosition(new BigDecimal("10"), new BigDecimal("200.00"));

        positionService.closePosition(account, ticker, position, new BigDecimal("10"), new BigDecimal("210.00"), null);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue().getRealizedPnl()).isEqualByComparingTo("100.00"); // (210-200)*10

        assertThat(position.getStatus()).isEqualTo(PositionStatus.CLOSED);
        assertThat(position.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(position.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("closing a long position for a loss computes a negative realized P&L")
    void closingLongPositionForLoss() {
        Position position = longPosition(new BigDecimal("10"), new BigDecimal("200.00"));

        positionService.closePosition(account, ticker, position, new BigDecimal("10"), new BigDecimal("190.00"), null);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue().getRealizedPnl()).isEqualByComparingTo("-100.00"); // (190-200)*10
    }

    @Test
    @DisplayName("closing a short position for a gain computes (entry - exit) * quantity, the mirror of a long")
    void closingShortPositionForGain() {
        Position position = shortPosition(new BigDecimal("10"), new BigDecimal("200.00"));

        // Price dropped after shorting — a gain for the short seller.
        positionService.closePosition(account, ticker, position, new BigDecimal("10"), new BigDecimal("190.00"), null);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepository).save(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue().getRealizedPnl()).isEqualByComparingTo("100.00"); // (200-190)*10
    }

    @Test
    @DisplayName("a partial close leaves the position PARTIAL_CLOSE with the correct remaining quantity")
    void partialCloseLeavesRemainderOpen() {
        Position position = longPosition(new BigDecimal("10"), new BigDecimal("200.00"));

        positionService.closePosition(account, ticker, position, new BigDecimal("4"), new BigDecimal("210.00"), null);

        assertThat(position.getStatus()).isEqualTo(PositionStatus.PARTIAL_CLOSE);
        assertThat(position.getRemainingQuantity()).isEqualByComparingTo("6");
        assertThat(position.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("closing releases entryPrice*quantity as capital, then posts realized P&L as a separate ledger entry")
    void closingPostsTwoLedgerEntries() {
        Position position = longPosition(new BigDecimal("10"), new BigDecimal("200.00"));

        positionService.closePosition(account, ticker, position, new BigDecimal("10"), new BigDecimal("210.00"), null);

        // Capital release: entryPrice(200) * qty(10) = 2000
        verify(ledgerService).postEntry(eq(account), any(), eq(new BigDecimal("2000.00")), any(), anyString());
        // Realized P&L: (210-200)*10 = 100, tagged with the trade reference
        verify(ledgerService).postEntry(eq(account), any(), eq(new BigDecimal("100.00")), any(), any(UUID.class), anyString());
    }

    @Test
    @DisplayName("a fill with no existing position opens a brand-new OPEN position and debits the ledger")
    void fillWithNoExistingPositionOpensNew() {
        when(positionRepository.findOpenPositionsForNetting(account.getId(), ticker.getId())).thenReturn(List.of());

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setSide(OrderSide.BUY);
        order.setType(OrderType.MARKET);
        order.setQuantity(new BigDecimal("10"));
        order.setStatus(OrderStatus.ROUTED);

        positionService.processFill(account, ticker, order, new BigDecimal("200.00"));

        ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(positionCaptor.capture());
        assertThat(positionCaptor.getValue().getStatus()).isEqualTo(PositionStatus.OPEN);
        assertThat(positionCaptor.getValue().getQuantity()).isEqualByComparingTo("10");

        // 10 * 200 = 2000, debited (negative)
        verify(ledgerService).postEntry(eq(account), any(), eq(new BigDecimal("-2000.00")), eq(order.getId()), anyString());
    }

    @Test
    @DisplayName("a SELL fill nets against an existing long instead of opening a new short")
    void sellFillNetsAgainstExistingLong() {
        Position existingLong = longPosition(new BigDecimal("10"), new BigDecimal("200.00"));
        when(positionRepository.findOpenPositionsForNetting(account.getId(), ticker.getId())).thenReturn(List.of(existingLong));

        Order sellOrder = new Order();
        sellOrder.setId(UUID.randomUUID());
        sellOrder.setSide(OrderSide.SELL);
        sellOrder.setType(OrderType.MARKET);
        sellOrder.setQuantity(new BigDecimal("10"));
        sellOrder.setStatus(OrderStatus.ROUTED);

        positionService.processFill(account, ticker, sellOrder, new BigDecimal("210.00"));

        // The existing long should have been closed, not left alone with a new short opened alongside it.
        assertThat(existingLong.getStatus()).isEqualTo(PositionStatus.CLOSED);
        verify(tradeRepository).save(any(Trade.class));

        // No brand-new position should be opened since the SELL exactly matched the long's quantity.
        verify(positionRepository, never()).save(argThat(p -> p != existingLong));
    }
}
