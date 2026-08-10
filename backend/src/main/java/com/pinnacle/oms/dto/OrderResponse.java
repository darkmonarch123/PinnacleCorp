package com.pinnacle.oms.dto;

import com.pinnacle.entity.Order;
import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.OrderStatus;
import com.pinnacle.entity.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String symbol,
    OrderSide side,
    OrderType type,
    OrderStatus status,
    BigDecimal quantity,
    BigDecimal filledQuantity,
    BigDecimal limitPrice,
    BigDecimal stopLoss,
    BigDecimal takeProfit,
    String rejectionReason,
    Instant createdAt
) {
    public static OrderResponse from(Order order, String symbol) {
        return new OrderResponse(
            order.getId(), symbol, order.getSide(), order.getType(), order.getStatus(),
            order.getQuantity(), order.getFilledQuantity(), order.getLimitPrice(),
            order.getStopLoss(), order.getTakeProfit(), order.getRejectionReason(), order.getCreatedAt()
        );
    }
}
