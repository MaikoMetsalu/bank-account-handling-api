package com.demo.bankaccounthandlingapi.unittest;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.entities.Account;
import com.demo.bankaccounthandlingapi.entities.Balance;
import com.demo.bankaccounthandlingapi.entities.TransactionLog;
import com.demo.bankaccounthandlingapi.enums.TransactionType;
import com.demo.bankaccounthandlingapi.exceptions.AccountNotFoundException;
import com.demo.bankaccounthandlingapi.exceptions.IllegalBalanceUpdateException;
import com.demo.bankaccounthandlingapi.repositories.AccountRepository;
import com.demo.bankaccounthandlingapi.repositories.BalanceRepository;
import com.demo.bankaccounthandlingapi.repositories.TransactionLogRepository;
import com.demo.bankaccounthandlingapi.services.BalanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private BalanceService balanceService;

    @Captor
    private ArgumentCaptor<Balance> balanceCaptor;

    @Captor
    private ArgumentCaptor<TransactionLog> transactionLogCaptor;

    private static final Long ACCOUNT_ID = 100L;
    private static final String CURRENCY_USD = "USD";

    @Test
    @DisplayName("Should successfully add money to an existing balance")
    void deposit_existingBalance_shouldAddSuccessfully() {
        // given
        BigDecimal initialAmount = new BigDecimal("50.00");
        BigDecimal depositAmount = new BigDecimal("25.00");

        Account account = new Account().setId(ACCOUNT_ID);
        Balance existingBalance = new Balance()
                .setAccount(account)
                .setCurrency(CURRENCY_USD)
                .setAmount(initialAmount);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, CURRENCY_USD))
                .thenReturn(Optional.of(existingBalance));

        // when
        BalanceResponse response = balanceService.deposit(ACCOUNT_ID, CURRENCY_USD, depositAmount);

        // then
        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(response.currency()).isEqualTo(CURRENCY_USD);

        verify(balanceRepository).save(balanceCaptor.capture());
        Balance savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));

        verify(transactionLogRepository).save(transactionLogCaptor.capture());
        TransactionLog savedLog = transactionLogCaptor.getValue();

        assertThat(savedLog.getAccount()).isEqualTo(account);
        assertThat(savedLog.getAmount()).isEqualTo(depositAmount);
        assertThat(savedLog.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(savedLog.getReferenceId()).isNotNull();
    }

    @Test
    @DisplayName("Should create new balance and add money")
    void deposit_givenNewBalance_createsBalanceAndAddsMoney() {
        // given
        BigDecimal depositAmount = new BigDecimal("100.00");
        Account account = new Account().setId(ACCOUNT_ID);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, CURRENCY_USD))
                .thenReturn(Optional.empty());

        when(balanceRepository.save(any(Balance.class))).then(returnsFirstArg());

        // when
        BalanceResponse response = balanceService.deposit(ACCOUNT_ID, CURRENCY_USD, depositAmount);

        // then
        assertThat(response.amount()).isEqualByComparingTo(depositAmount);

        verify(balanceRepository).save(balanceCaptor.capture());

        Balance finalSavedBalance = balanceCaptor.getValue();
        assertThat(finalSavedBalance.getAmount()).isEqualByComparingTo(depositAmount);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException if account is missing")
    void deposit_accountNotFound_throwsException() {
        // given
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() ->
                balanceService.deposit(ACCOUNT_ID, CURRENCY_USD, BigDecimal.TEN)
        )
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(String.valueOf(ACCOUNT_ID));

        verifyNoInteractions(balanceRepository);
        verifyNoInteractions(transactionLogRepository);
    }

    @ParameterizedTest(name = "Amount \"{0}\" should throw exception")
    @ValueSource(strings = {"0", "-10.00", "-0.01"})
    void deposit_nonPositiveAmount_throwsException(String amount) {
        // given
        Account account = new Account().setId(ACCOUNT_ID);
        Balance existingBalance = new Balance().setAccount(account).setAmount(BigDecimal.TEN);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(balanceRepository.findBalanceByAccountIdAndCurrency(any(), any()))
                .thenReturn(Optional.of(existingBalance));

        // when/then
        assertThatThrownBy(() ->
                balanceService.deposit(ACCOUNT_ID, CURRENCY_USD, new BigDecimal(amount))
        ).isInstanceOf(IllegalBalanceUpdateException.class);

        verify(balanceRepository, never()).save(any());
    }
}