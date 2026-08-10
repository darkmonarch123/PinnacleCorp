package com.pinnacle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Mock KYC only — no real document verification. */
public record KycRequest(
    @NotBlank String fullName,
    @NotNull LocalDate dateOfBirth,
    @NotBlank String country
) {}
