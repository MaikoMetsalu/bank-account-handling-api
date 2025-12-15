package com.demo.bankaccounthandlingapi.integrationtest;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.entities.Account;
import com.demo.bankaccounthandlingapi.exceptions.InsufficientFundsException;
import com.demo.bankaccounthandlingapi.repositories.AccountRepository;
import com.demo.bankaccounthandlingapi.repositories.BalanceRepository;
import com.demo.bankaccounthandlingapi.services.BalanceService;
import com.demo.bankaccounthandlingapi.services.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceServiceIntegrationTest extends IntegrationTest{

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    private Long accountId;

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();

        Account account = accountRepository.save(new Account());
        this.accountId = account.getId();
    }

    @Test
    @DisplayName("Get Balance: Returns correct amount for existing currency")
    void getBalance_shouldReturnStoredAmount() {
        balanceService.deposit(accountId, "USD", new BigDecimal("50.00"));

        BalanceResponse response = balanceService.getBalance(accountId, "USD");

        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Get Balance: Returns ZERO for non-existent currency")
    void getBalance_shouldReturnZeroForMissingCurrency() {
        BalanceResponse response = balanceService.getBalance(accountId, "JPY");

        assertThat(response.currency()).isEqualTo("JPY");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Exchange: Successfully converts USD to EUR")
    void exchange_shouldDebitSourceAndCreditTarget() {
        balanceService.deposit(accountId, "USD", new BigDecimal("100.00"));

        BalanceResponse response = balanceService.exchange(accountId, "USD", "EUR", new BigDecimal("50.00"));

        BigDecimal expectedEur = new BigDecimal("47.62");

        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.amount()).isEqualByComparingTo(expectedEur);

        var usdBalance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, "USD").orElseThrow();
        assertThat(usdBalance.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

        var eurBalance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, "EUR").orElseThrow();
        assertThat(eurBalance.getAmount()).isEqualByComparingTo(expectedEur);
    }

    @Test
    @DisplayName("Exchange: Fails if insufficient funds in source currency")
    void exchange_shouldFailUseInsufficientFunds() {
        balanceService.deposit(accountId, "USD", new BigDecimal("10.00"));

        assertThatThrownBy(() ->
                balanceService.exchange(accountId, "USD", "EUR", new BigDecimal("50.00"))
        ).isInstanceOf(InsufficientFundsException.class);

        var usdBalance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, "USD").orElseThrow();
        assertThat(usdBalance.getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));

        var eurBalance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, "EUR");
        assertThat(eurBalance).isEmpty();
    }

    @Test
    @DisplayName("Exchange: Fails for unsupported currency")
    void exchange_shouldFailForUnsupportedCurrency() {
        balanceService.deposit(accountId, "USD", new BigDecimal("100.00"));

        assertThatThrownBy(() ->
                balanceService.exchange(accountId, "USD", "BTC", new BigDecimal("10.00"))
        ).hasMessageContaining("Unsupported currency");
    }
}
