package com.pinnacle.entity;

import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.PositionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "positions")
@Getter
@Setter
public class Position {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "ticker_id", nullable = false)
    private UUID tickerId;

    @Column(name = "origin_order_id", nullable = false)
    private UUID originOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PositionStatus status = PositionStatus.OPEN;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "remaining_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal remainingQuantity;

    @Column(name = "entry_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal entryPrice;

    @Column(name = "stop_loss", precision = 18, scale = 4)
    private BigDecimal stopLoss;

    @Column(name = "take_profit", precision = 18, scale = 4)
    private BigDecimal takeProfit;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @PrePersist
    void onCreate() {
        openedAt = Instant.now();
    }
}
