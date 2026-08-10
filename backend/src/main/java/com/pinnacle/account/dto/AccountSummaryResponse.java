package com.pinnacle.account.dto;

import java.math.BigDecimal;

public record AccountSummaryResponse(
    String currency,
    BigDecimal balance,
    BigDecimal buyingPower,
    BigDecimal unrealizedPnl,
    BigDecimal equity // balance + unrealizedPnl
) {}
