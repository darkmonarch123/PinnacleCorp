package com.pinnacle.position.dto;

import java.math.BigDecimal;

/** Either field may be null to leave that value unchanged; send null explicitly to clear it. */
public record ModifyPositionRequest(
    BigDecimal stopLoss,
    BigDecimal takeProfit,
    boolean clearStopLoss,
    boolean clearTakeProfit
) {}
