package com.pinnacle.funding.dto;

import com.pinnacle.entity.CryptoFundingRequest;
import com.pinnacle.entity.enums.FundingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CryptoFundingResponse(
    UUID id,
    String cryptoType,
    String network,
    String walletAddress,
    String transactionHash,
    BigDecimal amount,
    BigDecimal usdEquivalent,
    FundingStatus status,
    String adminNote,
    Instant createdAt,
    Instant confirmedAt
) {
    public static CryptoFundingResponse from(CryptoFundingRequest r) {
        return new CryptoFundingResponse(
            r.getId(), r.getCryptoType(), r.getNetwork(), r.getWalletAddress(), r.getTransactionHash(),
            r.getAmount(), r.getUsdEquivalent(), r.getStatus(), r.getAdminNote(), r.getCreatedAt(), r.getConfirmedAt()
        );
    }
}
