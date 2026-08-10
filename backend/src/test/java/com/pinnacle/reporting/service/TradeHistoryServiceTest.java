package com.pinnacle.reporting.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.Trade;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.TickerRepository;
import com.pinnacle.repository.TradeRepository;
import com.pinnacle.reporting.dto.TradeStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeHistoryServiceTest {

    @Mock private TradeRepository tradeRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TickerRepository tickerRepository;

    private TradeHistoryService tradeHistoryService;
    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        tradeHistoryService = new TradeHistoryService(tradeRepository, accountRepository, tickerRepository, new BigDecimal("10000.00"));

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        Account account = new Account();
        account.setId(accountId);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
    }

    private Trade trade(BigDecimal pnl, Instant closedAt) {
        Trade t = new Trade();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setTickerId(UUID.randomUUID());
        t.setSide(OrderSide.BUY);
        t.setQuantity(BigDecimal.TEN);
        t.setEntryPrice(new BigDecimal("100"));
        t.setExitPrice(new BigDecimal("100").add(pnl.divide(BigDecimal.TEN)));
        t.setRealizedPnl(pnl);
        t.setOpenedAt(closedAt.minus(1, ChronoUnit.HOURS));
        t.setClosedAt(closedAt);
        return t;
    }

    @Test
    @DisplayName("win rate is winners divided by total trades, as a percentage")
    void computesWinRate() {
        Instant now = Instant.now();
        when(tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId)).thenReturn(List.of(
                trade(new BigDecimal("100"), now),
                trade(new BigDecimal("50"), now.plusSeconds(1)),
                trade(new BigDecimal("-30"), now.plusSeconds(2)),
                trade(new BigDecimal("-20"), now.plusSeconds(3))
        ));

        TradeStatsResponse stats = tradeHistoryService.getStats(userId);

        assertThat(stats.totalTrades()).isEqualTo(4);
        assertThat(stats.winningTrades()).isEqualTo(2);
        assertThat(stats.losingTrades()).isEqualTo(2);
        assertThat(stats.winRatePercent()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("avg win/loss ratio divides average winning P&L by average absolute losing P&L")
    void computesAvgWinLossRatio() {
        Instant now = Instant.now();
        when(tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId)).thenReturn(List.of(
                trade(new BigDecimal("100"), now),      // avg win = 100
                trade(new BigDecimal("-50"), now.plusSeconds(1)) // avg loss = 50
        ));

        TradeStatsResponse stats = tradeHistoryService.getStats(userId);

        assertThat(stats.avgWinLossRatio()).isEqualByComparingTo("2.00"); // 100/50
    }

    @Test
    @DisplayName("avg win/loss ratio is null when there are no losing trades to divide by")
    void avgWinLossRatioIsNullWithNoLosses() {
        when(tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId))
                .thenReturn(List.of(trade(new BigDecimal("100"), Instant.now())));

        TradeStatsResponse stats = tradeHistoryService.getStats(userId);

        assertThat(stats.avgWinLossRatio()).isNull();
    }

    @Test
    @DisplayName("max drawdown reflects the largest peak-to-trough drop in cumulative equity")
    void computesMaxDrawdown() {
        Instant now = Instant.now();
        // Starting balance 10000 -> +1000 (11000, new peak) -> -2000 (9000, drawdown from 11000)
        when(tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId)).thenReturn(List.of(
                trade(new BigDecimal("1000"), now),
                trade(new BigDecimal("-2000"), now.plusSeconds(1))
        ));

        TradeStatsResponse stats = tradeHistoryService.getStats(userId);

        // drawdown = (11000 - 9000) / 11000 * 100 = 18.18%
        assertThat(stats.maxDrawdownPercent()).isEqualByComparingTo("18.18");
    }

    @Test
    @DisplayName("an account with no closed trades reports zeroed-out stats rather than dividing by zero")
    void handlesNoTradesGracefully() {
        when(tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId)).thenReturn(List.of());

        TradeStatsResponse stats = tradeHistoryService.getStats(userId);

        assertThat(stats.totalTrades()).isZero();
        assertThat(stats.winRatePercent()).isEqualByComparingTo("0");
        assertThat(stats.avgWinLossRatio()).isNull();
        assertThat(stats.maxDrawdownPercent()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("the equity curve starts at the starting balance and accumulates realized P&L chronologically")
    void equityCurveAccumulatesChronologically() {
        Instant now = Instant.now();
        // Trades returned out of chronological order — the curve must still sort by closedAt.
        when(tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId)).thenReturn(List.of(
                trade(new BigDecimal("-100"), now.plusSeconds(1)),
                trade(new BigDecimal("500"), now)
        ));

        var curve = tradeHistoryService.getEquityCurve(userId);

        assertThat(curve).hasSize(3); // starting point + 2 trades
        assertThat(curve.get(0).equity()).isEqualByComparingTo("10000.00");
        assertThat(curve.get(1).equity()).isEqualByComparingTo("10500.00"); // 10000 + 500 (the earlier trade)
        assertThat(curve.get(2).equity()).isEqualByComparingTo("10400.00"); // 10500 - 100 (the later trade)
    }

    @Test
    @DisplayName("the equity curve for an account with no trades is just the starting-balance point")
    void equityCurveWithNoTradesIsJustStartingPoint() {
        when(tradeRepository.findByAccountIdOrderByClosedAtDesc(accountId)).thenReturn(List.of());

        var curve = tradeHistoryService.getEquityCurve(userId);

        assertThat(curve).hasSize(1);
        assertThat(curve.get(0).equity()).isEqualByComparingTo("10000.00");
    }
}
