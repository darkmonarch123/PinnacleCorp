package com.pinnacle.marketdata.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Normalized quote shape, independent of which upstream provider returned it. */
public record PriceQuote(
    String symbol,
    BigDecimal price,
    BigDecimal volume,
    Instant timestamp
) {}
