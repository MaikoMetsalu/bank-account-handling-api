package com.demo.bankaccounthandlingapi.controllers;

import com.demo.bankaccounthandlingapi.controllers.specs.BalanceControllerSpec;
import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.dtos.DepositRequest;
import com.demo.bankaccounthandlingapi.dtos.ExchangeRequest;
import com.demo.bankaccounthandlingapi.dtos.WithdrawRequest;
import com.demo.bankaccounthandlingapi.services.AccountOperationService;
import com.demo.bankaccounthandlingapi.services.BalanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/{accountId}/balance")
public class BalanceController implements BalanceControllerSpec {

    private final BalanceService balanceService;
    private final AccountOperationService accountOperationService;

    public BalanceController(BalanceService balanceService, AccountOperationService accountOperationService) {
        this.balanceService = balanceService;
        this.accountOperationService = accountOperationService;
    }

    @PostMapping("/deposit")
    public BalanceResponse deposit(@PathVariable Long accountId, @RequestBody @Valid DepositRequest depositRequest) {
        return balanceService.deposit(accountId, depositRequest.currency(), depositRequest.amount());
    }

    @PostMapping("/withdraw")
    public BalanceResponse withdraw(@PathVariable Long accountId, @RequestBody @Valid WithdrawRequest withdrawRequest) {
        return accountOperationService.debitAccount(accountId, withdrawRequest.currency(), withdrawRequest.amount());
    }

    @GetMapping
    public BalanceResponse getBalance(@PathVariable Long accountId, @RequestParam String currency) {
        return balanceService.getBalance(accountId, currency);
    }

    @PostMapping("/exchange")
    public BalanceResponse exchange(@PathVariable Long accountId, @RequestBody @Valid ExchangeRequest exchangeRequest) {
        return balanceService.exchange(accountId, exchangeRequest.fromCurrency(), exchangeRequest.toCurrency(), exchangeRequest.amount());
    }
}
