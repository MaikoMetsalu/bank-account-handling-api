package com.demo.bankaccounthandlingapi.integrationtest;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.entities.Account;
import com.demo.bankaccounthandlingapi.exceptions.ExternalSystemException;
import com.demo.bankaccounthandlingapi.repositories.AccountRepository;
import com.demo.bankaccounthandlingapi.repositories.BalanceRepository;
import com.demo.bankaccounthandlingapi.services.AccountOperationService;
import com.demo.bankaccounthandlingapi.services.BalanceService;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountFlowIntegrationTest extends IntegrationTest {

    @Autowired
    private AccountOperationService accountOperationService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    private Long accountId;
    private static final String CURRENCY = "USD";

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();

        Account account = accountRepository.save(new Account());
        this.accountId = account.getId();
    }

    @Test
    @DisplayName("Deposit, then debit with external logging success")
    void shouldHandleDepositAndDebitCycle() {
        wireMockServer.stubFor(WireMock.get("/")
                .willReturn(WireMock.ok()));

        balanceService.deposit(accountId, CURRENCY, new BigDecimal("100.00"));

        BalanceResponse response = accountOperationService.debitAccount(accountId, CURRENCY, new BigDecimal("40.00"));

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(response.currency()).isEqualTo(CURRENCY);

        var balance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, CURRENCY)
                .orElseThrow(() -> new AssertionError("Balance should exist"));

        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(balance.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("External audit failure blocks debit operation")
    void shouldBlockDebitIfExternalServiceFails() {
        wireMockServer.stubFor(WireMock.get("/")
                .willReturn(WireMock.serverError()));

        balanceService.deposit(accountId, CURRENCY, new BigDecimal("100.00"));

        assertThatThrownBy(() ->
                accountOperationService.debitAccount(accountId, CURRENCY, new BigDecimal("40.00"))
        ).isInstanceOf(ExternalSystemException.class);

        var balance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, CURRENCY).orElseThrow();
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Concurrent requests do not allow double spending")
    void shouldHandleConcurrentDebits() {
        wireMockServer.stubFor(WireMock.get("/").willReturn(WireMock.ok()));

        balanceService.deposit(accountId, CURRENCY, new BigDecimal("100.00"));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        CompletableFuture<?>[] futures = new CompletableFuture[threads];

        for (int i = 0; i < threads; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    accountOperationService.debitAccount(accountId, CURRENCY, new BigDecimal("60.00"));
                    successCount.incrementAndGet();
                } catch (Exception _) {
                    failCount.incrementAndGet();
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).join();

        assertThat(successCount.get())
                .withFailMessage("Expected exactly 1 success, but got %d", successCount.get())
                .isEqualTo(1);

        assertThat(failCount.get()).isEqualTo(1);

        var balance = balanceRepository.findBalanceByAccountIdAndCurrency(accountId, CURRENCY).get();
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

}