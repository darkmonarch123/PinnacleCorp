package com.pinnacle.funding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateCryptoFundingRequest(
    @NotBlank String cryptoType,
    @NotBlank String network,
    @NotBlank String walletAddress,
    @NotBlank String transactionHash,
    @NotNull @DecimalMin(value = "0.00000001") BigDecimal amount
) {}
