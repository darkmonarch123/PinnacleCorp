package com.pinnacle.entity;

import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.OrderStatus;
import com.pinnacle.entity.enums.OrderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "ticker_id", nullable = false)
    private UUID tickerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "filled_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(name = "limit_price", precision = 18, scale = 4)
    private BigDecimal limitPrice;

    @Column(name = "stop_loss", precision = 18, scale = 4)
    private BigDecimal stopLoss;

    @Column(name = "take_profit", precision = 18, scale = 4)
    private BigDecimal takeProfit;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
