package com.pinnacle.oms.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RiskCheckServiceTest {

    private RiskCheckService riskCheckService;
    private Account account;
    private Ticker ticker;

    @BeforeEach
    void setUp() {
        riskCheckService = new RiskCheckService();

        account = new Account();
        account.setBuyingPower(new BigDecimal("10000.00"));

        ticker = new Ticker();
        ticker.setSymbol("AAPL");
        ticker.setActive(true);
        ticker.setMinOrderSize(new BigDecimal("1"));
        ticker.setMaxOrderSize(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("passes when buying power, quantity, and ticker are all valid")
    void passesForValidOrder() {
        var result = riskCheckService.checkOrder(account, ticker, new BigDecimal("10"), new BigDecimal("100"));

        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).isNull();
    }

    @Test
    @DisplayName("rejects when the ticker is not active")
    void rejectsInactiveTicker() {
        ticker.setActive(false);

        var result = riskCheckService.checkOrder(account, ticker, new BigDecimal("10"), new BigDecimal("100"));

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("not currently tradable");
    }

    @Test
    @DisplayName("rejects when quantity is below the ticker's minimum order size")
    void rejectsQuantityBelowMinimum() {
        ticker.setMinOrderSize(new BigDecimal("5"));

        var result = riskCheckService.checkOrder(account, ticker, new BigDecimal("1"), new BigDecimal("100"));

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("below minimum order size");
    }

    @Test
    @DisplayName("rejects when quantity exceeds the ticker's maximum order size")
    void rejectsQuantityAboveMaximum() {
        ticker.setMaxOrderSize(new BigDecimal("50"));

        var result = riskCheckService.checkOrder(account, ticker, new BigDecimal("100"), new BigDecimal("100"));

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("exceeds maximum order size");
    }

    @Test
    @DisplayName("rejects when the order's notional exceeds available buying power")
    void rejectsInsufficientBuyingPower() {
        account.setBuyingPower(new BigDecimal("500.00"));

        // 10 shares @ $100 = $1000 notional > $500 buying power
        var result = riskCheckService.checkOrder(account, ticker, new BigDecimal("10"), new BigDecimal("100"));

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("Insufficient buying power");
    }

    @Test
    @DisplayName("accepts an order whose notional exactly equals buying power")
    void acceptsExactBuyingPowerMatch() {
        account.setBuyingPower(new BigDecimal("1000.00"));

        var result = riskCheckService.checkOrder(account, ticker, new BigDecimal("10"), new BigDecimal("100"));

        assertThat(result.passed()).isTrue();
    }
}
