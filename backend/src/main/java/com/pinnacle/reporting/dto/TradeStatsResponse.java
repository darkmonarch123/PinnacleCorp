package com.pinnacle.reporting.dto;

import java.math.BigDecimal;

public record TradeStatsResponse(
    int totalTrades,
    int winningTrades,
    int losingTrades,
    BigDecimal winRatePercent,
    BigDecimal avgWinLossRatio, // null if there are no losing trades to divide by
    BigDecimal maxDrawdownPercent,
    BigDecimal totalRealizedPnl
) {}
