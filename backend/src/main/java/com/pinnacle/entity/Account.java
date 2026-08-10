package com.pinnacle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Balance/buyingPower/marginUsed are DERIVED CACHES for fast reads.
 * The source of truth is always the sum of ledger_entries for this account.
 * Never mutate these fields directly outside the ledger-posting service.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Fixed/mock conversion only (currency_rates table) — not live FX. Ticker
    // prices remain USD; no automatic conversion happens when computing
    // buying power against non-USD accounts. See FINAL_HANDOFF.md.
    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = new BigDecimal("10000.00");

    @Column(name = "buying_power", nullable = false, precision = 18, scale = 2)
    private BigDecimal buyingPower = new BigDecimal("10000.00");

    @Column(name = "margin_used", nullable = false, precision = 18, scale = 2)
    private BigDecimal marginUsed = BigDecimal.ZERO;

    @Column(name = "is_demo", nullable = false)
    private boolean isDemo = true;

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
