package com.pinnacle.entity;

import com.pinnacle.entity.enums.FundingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Simulated crypto funding. No real blockchain custody, wallet signing, or
 * on-chain verification happens here — this only records what the user
 * claims they sent, for administrator review. Never stores private keys.
 */
@Entity
@Table(name = "crypto_funding_requests")
@Getter
@Setter
public class CryptoFundingRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "crypto_type", nullable = false)
    private String cryptoType; // e.g. "BTC", "ETH", "USDT"

    @Column(nullable = false)
    private String network; // e.g. "Bitcoin", "Ethereum", "Tron (TRC20)"

    @Column(name = "wallet_address", nullable = false)
    private String walletAddress; // the sending wallet address the user claims

    @Column(name = "transaction_hash", nullable = false, unique = true)
    private String transactionHash;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal amount; // in the crypto asset's own units, not USD

    @Column(name = "usd_equivalent", precision = 18, scale = 2)
    private BigDecimal usdEquivalent; // admin-entered at confirm time, credited to the ledger

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
