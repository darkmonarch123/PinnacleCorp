package com.pinnacle.account.dto;

import java.math.BigDecimal;

/** usdRate: 1 unit of `currency` = usdRate USD. Fixed/mock rates, not live FX. */
public record CurrencyRateResponse(String currency, BigDecimal usdRate) {}
