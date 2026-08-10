package com.pinnacle.position.dto;

import com.pinnacle.entity.Position;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.PositionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PositionResponse(
    UUID id,
    String symbol,
    OrderSide side,
    PositionStatus status,
    BigDecimal quantity,
    BigDecimal remainingQuantity,
    BigDecimal entryPrice,
    BigDecimal stopLoss,
    BigDecimal takeProfit,
    Instant openedAt,
    Instant closedAt
) {
    public static PositionResponse from(Position p, String symbol) {
        return new PositionResponse(
            p.getId(), symbol, p.getSide(), p.getStatus(), p.getQuantity(), p.getRemainingQuantity(),
            p.getEntryPrice(), p.getStopLoss(), p.getTakeProfit(), p.getOpenedAt(), p.getClosedAt()
        );
    }
}
