package com.demo.bankaccounthandlingapi.dtos;

import java.math.BigDecimal;

public record BalanceResponse(
        String currency,
        BigDecimal amount
) {}
