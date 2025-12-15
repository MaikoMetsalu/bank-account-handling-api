package com.demo.bankaccounthandlingapi.services;

import com.demo.bankaccounthandlingapi.exceptions.CurrencyExchangeException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Map;

@Service
public class ExchangeRateService {
    private static final Map<Currency, BigDecimal> RATES_TO_EUR = Map.of(
            Currency.getInstance("EUR"), BigDecimal.ONE,
            Currency.getInstance("USD"), new BigDecimal("1.05"),
            Currency.getInstance("SEK"), new BigDecimal("11.50"),
            Currency.getInstance("RUB"), new BigDecimal("105.00")
    );

    public BigDecimal convert(Currency fromCurrency, Currency toCurrency, BigDecimal amount) {
        if (!RATES_TO_EUR.containsKey(fromCurrency) || !RATES_TO_EUR.containsKey(toCurrency)) {
            throw new CurrencyExchangeException(fromCurrency, toCurrency);
        }

        BigDecimal fromRate = RATES_TO_EUR.get(fromCurrency);
        BigDecimal toRate = RATES_TO_EUR.get(toCurrency);

        // Formula: Amount * (ToRate / FromRate)
        // 10 * (11.50 / 1.05) = 109.52 SEK
        BigDecimal ratio = toRate.divide(fromRate, MathContext.DECIMAL128);

        return amount.multiply(ratio).setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean isSupported(Currency currency) {
        return RATES_TO_EUR.containsKey(currency);
    }
}
