package com.demo.bankaccounthandlingapi.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExchangeRequest(
    @NotNull
    @Schema(description = "Source Currency", example = "USD")
    String fromCurrency,
    @NotNull
    @Schema(description = "Target Currency", example = "EUR")
    String toCurrency,
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 19, fraction = 2, message = "Invalid precision")
    @Schema(description = "Amount to exchange", example = "100.50")
    BigDecimal amount) { }
