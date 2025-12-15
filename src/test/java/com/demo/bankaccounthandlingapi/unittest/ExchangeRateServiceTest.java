package com.demo.bankaccounthandlingapi.unittest;

import com.demo.bankaccounthandlingapi.services.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    private final ExchangeRateService service = new ExchangeRateService();

    @Test
    void convert_eurToUsd_correctMath() {
        // 100 EUR -> USD (Rate 1.05)
        BigDecimal result = service.convert(Currency.getInstance("EUR"), Currency.getInstance("USD"), new BigDecimal("100.00"));

        // 100 * 1.05 = 105.00
        assertThat(result).isEqualByComparingTo(new BigDecimal("105.00"));
    }

    @Test
    void convert_usdToSek_crossRate() {
        // 10 USD -> SEK
        // Base: EUR = 1, USD = 1.05, SEK = 11.50
        BigDecimal result = service.convert(Currency.getInstance("USD"), Currency.getInstance("SEK"), new BigDecimal("10.00"));

        assertThat(result).isEqualByComparingTo(new BigDecimal("109.52"));
    }
}