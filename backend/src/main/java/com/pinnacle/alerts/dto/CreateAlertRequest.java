package com.pinnacle.alerts.dto;

import com.pinnacle.entity.enums.AlertCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAlertRequest(
    @NotBlank String symbol,
    @NotNull BigDecimal targetPrice,
    @NotNull AlertCondition condition
) {}
