package com.demo.bankaccounthandlingapi.exceptions;

import java.util.Currency;

public class CurrencyExchangeException extends RuntimeException {
    public CurrencyExchangeException(Currency from, Currency to) {
        super("Can not exchange from %s to %s".formatted(from.getCurrencyCode(), to.getCurrencyCode()));
    }

    public CurrencyExchangeException(String message) {
        super(message);
    }
}
