package com.pinnacle.oms.dto;

import com.pinnacle.entity.enums.OrderSide;
import com.pinnacle.entity.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceOrderRequest(
    @NotBlank String symbol,
    @NotNull OrderSide side,
    @NotNull OrderType type,
    @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
    BigDecimal limitPrice,   // required when type == LIMIT
    BigDecimal stopLoss,     // optional
    BigDecimal takeProfit    // optional
) {}
