package com.demo.bankaccounthandlingapi.exceptions;

public class IllegalBalanceUpdateException extends RuntimeException {
    public IllegalBalanceUpdateException(Long accountId) {
        super("Invalid balance update for account id %d ".formatted(accountId));
    }
}
