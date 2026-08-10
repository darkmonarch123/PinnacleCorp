package com.pinnacle.alerts.dto;

import com.pinnacle.entity.PriceAlert;
import com.pinnacle.entity.enums.AlertCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceAlertResponse(
    UUID id,
    String symbol,
    BigDecimal targetPrice,
    AlertCondition condition,
    boolean active,
    Instant triggeredAt,
    Instant createdAt
) {
    public static PriceAlertResponse from(PriceAlert alert, String symbol) {
        return new PriceAlertResponse(
            alert.getId(), symbol, alert.getTargetPrice(), alert.getCondition(),
            alert.isActive(), alert.getTriggeredAt(), alert.getCreatedAt()
        );
    }
}
