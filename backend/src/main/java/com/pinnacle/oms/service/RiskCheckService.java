package com.pinnacle.oms.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.Ticker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiskCheckService {

    public record RiskCheckResult(boolean passed, String reason) {
        static RiskCheckResult pass() {
            return new RiskCheckResult(true, null);
        }
        static RiskCheckResult fail(String reason) {
            return new RiskCheckResult(false, reason);
        }
    }

    /**
     * Buying power is checked for both BUY and SELL: this simulator doesn't
     * yet model margin differently for short positions, so opening any
     * position — long or short — reserves cash against the order's notional
     * value. Netting against an existing opposite position happens in
     * position management, not here.
     */
    public RiskCheckResult checkOrder(Account account, Ticker ticker, BigDecimal quantity, BigDecimal estimatedPrice) {
        if (!ticker.isActive()) {
            return RiskCheckResult.fail("Ticker " + ticker.getSymbol() + " is not currently tradable");
        }

        if (quantity.compareTo(ticker.getMinOrderSize()) < 0) {
            return RiskCheckResult.fail("Quantity below minimum order size of " + ticker.getMinOrderSize());
        }

        if (quantity.compareTo(ticker.getMaxOrderSize()) > 0) {
            return RiskCheckResult.fail("Quantity exceeds maximum order size of " + ticker.getMaxOrderSize());
        }

        BigDecimal estimatedCost = quantity.multiply(estimatedPrice);
        if (account.getBuyingPower().compareTo(estimatedCost) < 0) {
            return RiskCheckResult.fail("Insufficient buying power: need " + estimatedCost + ", have " + account.getBuyingPower());
        }

        return RiskCheckResult.pass();
    }
}
