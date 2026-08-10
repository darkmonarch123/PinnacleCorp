package com.pinnacle.marketdata.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Payload broadcast over STOMP to /topic/prices/{symbol}. */
public record PriceTickMessage(
    String symbol,
    BigDecimal price,
    BigDecimal changePercent,
    Instant timestamp
) {}
