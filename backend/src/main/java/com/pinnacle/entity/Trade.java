package com.pinnacle.entity;

import com.pinnacle.entity.enums.OrderSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trades")
@Getter
@Setter
public class Trade {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "ticker_id", nullable = false)
    private UUID tickerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "entry_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal entryPrice;

    @Column(name = "exit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal exitPrice;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 2)
    private BigDecimal realizedPnl;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;

    @PrePersist
    void onCreate() {
        if (closedAt == null) closedAt = Instant.now();
    }
}
