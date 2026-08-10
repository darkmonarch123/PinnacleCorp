package com.pinnacle.entity;

import com.pinnacle.entity.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only. Account.balance is a cache derived from summing these rows;
 * this table is the single source of truth for every balance-changing event.
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
public class LedgerEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private LedgerEntryType entryType;

    /** Signed: positive = credit, negative = debit. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_order_id")
    private UUID referenceOrderId;

    @Column(name = "reference_trade_id")
    private UUID referenceTradeId;

    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
