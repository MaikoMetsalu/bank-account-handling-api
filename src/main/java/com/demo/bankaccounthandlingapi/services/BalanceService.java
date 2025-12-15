package com.demo.bankaccounthandlingapi.services;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.entities.Balance;
import com.demo.bankaccounthandlingapi.enums.TransactionType;
import com.demo.bankaccounthandlingapi.exceptions.AccountNotFoundException;
import com.demo.bankaccounthandlingapi.exceptions.InsufficientFundsException;
import com.demo.bankaccounthandlingapi.repositories.AccountRepository;
import com.demo.bankaccounthandlingapi.repositories.BalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final AccountRepository accountRepository;
    private final TransactionLogService transactionLogService;

    public BalanceService(BalanceRepository balanceRepository, AccountRepository accountRepository, TransactionLogService transactionLogService) {
        this.balanceRepository = balanceRepository;
        this.accountRepository = accountRepository;
        this.transactionLogService = transactionLogService;
    }

    @Transactional
    public BalanceResponse deposit(Long accountId, String currency, BigDecimal amount) {
        final var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Balance balance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, currency)
                .orElseGet(() -> new Balance()
                        .setAmount(BigDecimal.ZERO)
                        .setCurrency(currency)
                        .setAccount(account));

        balance.deposit(amount);
        balanceRepository.save(balance);

        transactionLogService.logTransaction(account, TransactionType.DEPOSIT, amount, currency);

        return new BalanceResponse(balance.getCurrency(), balance.getAmount());
    }

    @Transactional
    public BalanceResponse debit(Long accountId, String currency, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        final var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Balance balance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, currency)
                .orElseThrow(() -> new InsufficientFundsException(accountId, currency));

        balance.debit(amount);

        balanceRepository.save(balance);

        transactionLogService.logTransaction(account, TransactionType.WITHDRAWAL, amount, currency);

        return new BalanceResponse(balance.getCurrency(), balance.getAmount());
    }
}
