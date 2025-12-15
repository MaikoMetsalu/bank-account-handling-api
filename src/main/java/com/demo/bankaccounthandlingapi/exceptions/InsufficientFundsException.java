package com.demo.bankaccounthandlingapi.exceptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long accountId, String currency) {
        super("Insufficient funds in account with id %s for currency: %s".formatted(accountId, currency));
    }
}
