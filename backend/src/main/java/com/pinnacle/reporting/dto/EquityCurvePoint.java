package com.pinnacle.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Equity here is starting balance + cumulative realized P&L at each closed
 * trade — it does not mark open positions to market, so it understates
 * total equity while positions are open. See README.
 */
public record EquityCurvePoint(Instant time, BigDecimal equity) {}
