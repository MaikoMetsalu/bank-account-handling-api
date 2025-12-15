package com.demo.bankaccounthandlingapi.services;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.entities.Account;
import com.demo.bankaccounthandlingapi.entities.Balance;
import com.demo.bankaccounthandlingapi.enums.TransactionType;
import com.demo.bankaccounthandlingapi.exceptions.AccountNotFoundException;
import com.demo.bankaccounthandlingapi.exceptions.CurrencyExchangeException;
import com.demo.bankaccounthandlingapi.exceptions.InsufficientFundsException;
import com.demo.bankaccounthandlingapi.repositories.AccountRepository;
import com.demo.bankaccounthandlingapi.repositories.BalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;

@Service
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final AccountRepository accountRepository;
    private final TransactionLogService transactionLogService;
    private final ExchangeRateService exchangeRateService;

    public BalanceService(BalanceRepository balanceRepository, AccountRepository accountRepository, TransactionLogService transactionLogService, ExchangeRateService exchangeRateService) {
        this.balanceRepository = balanceRepository;
        this.accountRepository = accountRepository;
        this.transactionLogService = transactionLogService;
        this.exchangeRateService = exchangeRateService;
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

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long accountId, String currency) {
        return balanceRepository.findBalanceByAccountIdAndCurrency(accountId, currency)
                .map(it -> new BalanceResponse(it.getCurrency(), it.getAmount()))
                .orElse(new BalanceResponse(currency, BigDecimal.ZERO));
    }

    @Transactional
    public BalanceResponse exchange(Long accountId, String fromCurrency, String toCurrency, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CurrencyExchangeException("Exchange amount must be positive");
        }

        validateCurrencySupport(fromCurrency);
        validateCurrencySupport(toCurrency);

        if (fromCurrency.equals(toCurrency)) {
            throw new CurrencyExchangeException("Cannot exchange same currency");
        }
        if (!exchangeRateService.isSupported(Currency.getInstance(fromCurrency)) || !exchangeRateService.isSupported(Currency.getInstance(toCurrency))) {
            throw new CurrencyExchangeException("The currency pair %s / %s is not supported".formatted(fromCurrency, toCurrency));
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Balance sourceBalance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, fromCurrency)
                .orElseThrow(() -> new InsufficientFundsException(accountId, fromCurrency));

        sourceBalance.debit(amount);
        balanceRepository.save(sourceBalance);

        BigDecimal convertedAmount = exchangeRateService.convert(Currency.getInstance(fromCurrency), Currency.getInstance(toCurrency), amount);

        Balance targetBalance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, toCurrency)
                .orElseGet(() -> new Balance()
                        .setAccount(account)
                        .setCurrency(toCurrency)
                        .setAmount(BigDecimal.ZERO));

        targetBalance.deposit(convertedAmount);
        balanceRepository.save(targetBalance);

        transactionLogService.logTransaction(account, TransactionType.EXCHANGE_OUT, amount, fromCurrency);
        transactionLogService.logTransaction(account, TransactionType.EXCHANGE_IN, convertedAmount, toCurrency);

        return new BalanceResponse(targetBalance.getCurrency(), targetBalance.getAmount());
    }

    private void validateCurrencySupport(String currency) {
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new CurrencyExchangeException("Unsupported currency code: %s".formatted(currency));
        }
    }
}
