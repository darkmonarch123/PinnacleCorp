package com.pinnacle.entity;

import com.pinnacle.entity.enums.FundingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Simulated bank-transfer funding, per the final-project scope: no real
 * banking integration exists, so confirmation is administrator-gated
 * (User.isAdmin) rather than automatic. Confirming posts a DEPOSIT ledger
 * entry via LedgerService — this table never touches Account.balance itself.
 */
@Entity
@Table(name = "bank_transfer_requests")
@Getter
@Setter
public class BankTransferRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "transfer_reference", nullable = false, unique = true)
    private String transferReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FundingStatus status = FundingStatus.PENDING;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
