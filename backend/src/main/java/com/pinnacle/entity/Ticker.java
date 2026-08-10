package com.pinnacle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickers")
@Getter
@Setter
public class Ticker {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String exchange;

    private String sector;

    @Column(name = "min_order_size", nullable = false, precision = 18, scale = 4)
    private BigDecimal minOrderSize = BigDecimal.ONE;

    @Column(name = "max_order_size", nullable = false, precision = 18, scale = 4)
    private BigDecimal maxOrderSize = new BigDecimal("100000");

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
