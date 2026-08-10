package com.pinnacle.reporting.dto;

import com.pinnacle.entity.Trade;
import com.pinnacle.entity.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeResponse(
    UUID id,
    String symbol,
    OrderSide side,
    BigDecimal quantity,
    BigDecimal entryPrice,
    BigDecimal exitPrice,
    BigDecimal realizedPnl,
    Instant openedAt,
    Instant closedAt
) {
    public static TradeResponse from(Trade t, String symbol) {
        return new TradeResponse(
            t.getId(), symbol, t.getSide(), t.getQuantity(), t.getEntryPrice(),
            t.getExitPrice(), t.getRealizedPnl(), t.getOpenedAt(), t.getClosedAt()
        );
    }
}
