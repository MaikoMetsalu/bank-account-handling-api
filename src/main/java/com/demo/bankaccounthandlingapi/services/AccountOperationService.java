package com.demo.bankaccounthandlingapi.services;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.external.ExternalLoggingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountOperationService {

    private final ExternalLoggingService externalLoggingService;
    private final BalanceService balanceService;

    public AccountOperationService(ExternalLoggingService externalLoggingService, BalanceService balanceService) {
        this.externalLoggingService = externalLoggingService;
        this.balanceService = balanceService;
    }

    public BalanceResponse debitAccount(Long accountId, String currency, BigDecimal amount) {
        externalLoggingService.sendLog();

        return balanceService.debit(accountId, currency, amount);
    }

}
