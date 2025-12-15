package com.demo.bankaccounthandlingapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
    @NotNull
    @Schema(description = "Currency Code", example = "USD")
    String currency,
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 19, fraction = 2, message = "Invalid precision")
    @Schema(description = "Amount to deposit", example = "100.50")
    BigDecimal amount) { }
