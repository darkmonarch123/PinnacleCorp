package com.pinnacle.marketdata.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CandleDto(
    Instant time,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume
) {}
