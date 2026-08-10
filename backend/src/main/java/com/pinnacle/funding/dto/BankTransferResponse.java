package com.pinnacle.funding.dto;

import com.pinnacle.entity.BankTransferRequest;
import com.pinnacle.entity.enums.FundingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BankTransferResponse(
    UUID id,
    BigDecimal amount,
    String currency,
    String transferReference,
    FundingStatus status,
    String adminNote,
    Instant createdAt,
    Instant confirmedAt
) {
    public static BankTransferResponse from(BankTransferRequest r) {
        return new BankTransferResponse(
            r.getId(), r.getAmount(), r.getCurrency(), r.getTransferReference(),
            r.getStatus(), r.getAdminNote(), r.getCreatedAt(), r.getConfirmedAt()
        );
    }
}
