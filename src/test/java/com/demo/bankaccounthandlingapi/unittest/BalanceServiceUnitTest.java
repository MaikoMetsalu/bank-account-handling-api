package com.demo.bankaccounthandlingapi.unittest;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.entities.Account;
import com.demo.bankaccounthandlingapi.entities.Balance;
import com.demo.bankaccounthandlingapi.enums.TransactionType;
import com.demo.bankaccounthandlingapi.exceptions.AccountNotFoundException;
import com.demo.bankaccounthandlingapi.exceptions.CurrencyExchangeException;
import com.demo.bankaccounthandlingapi.exceptions.IllegalBalanceUpdateException;
import com.demo.bankaccounthandlingapi.exceptions.InsufficientFundsException;
import com.demo.bankaccounthandlingapi.repositories.AccountRepository;
import com.demo.bankaccounthandlingapi.repositories.BalanceRepository;
import com.demo.bankaccounthandlingapi.services.BalanceService;
import com.demo.bankaccounthandlingapi.services.ExchangeRateService;
import com.demo.bankaccounthandlingapi.services.TransactionLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
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
    private TransactionLogService transactionLogService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private BalanceService balanceService;

    @Captor
    private ArgumentCaptor<Balance> balanceCaptor;

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

        verify(transactionLogService).logTransaction(account, TransactionType.DEPOSIT, depositAmount, CURRENCY_USD);
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
        verifyNoInteractions(transactionLogService);
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

    @Test
    @DisplayName("Should successfully debit account with sufficient funds")
    void debit_sufficientFunds_shouldDeductSuccessfully() {
        // given
        BigDecimal initialAmount = new BigDecimal("100.00");
        BigDecimal debitAmount = new BigDecimal("40.00");
        BigDecimal expectedBalance = new BigDecimal("60.00");

        Account account = new Account().setId(ACCOUNT_ID);
        Balance existingBalance = new Balance()
                .setAccount(account)
                .setCurrency(CURRENCY_USD)
                .setAmount(initialAmount);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, CURRENCY_USD))
                .thenReturn(Optional.of(existingBalance));

        // when
        BalanceResponse response = balanceService.debit(ACCOUNT_ID, CURRENCY_USD, debitAmount);

        // then
        assertThat(response.amount()).isEqualByComparingTo(expectedBalance);

        verify(balanceRepository).save(balanceCaptor.capture());
        Balance savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getAmount()).isEqualByComparingTo(expectedBalance);

        verify(transactionLogService).logTransaction(account, TransactionType.WITHDRAWAL, debitAmount, CURRENCY_USD);
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when balance is too low")
    void debit_insufficientFunds_throwsException() {
        // given
        BigDecimal initialAmount = new BigDecimal("10.00");
        BigDecimal debitAmount = new BigDecimal("50.00");

        Account account = new Account().setId(ACCOUNT_ID);
        Balance existingBalance = new Balance()
                .setAccount(account)
                .setCurrency(CURRENCY_USD)
                .setAmount(initialAmount);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, CURRENCY_USD))
                .thenReturn(Optional.of(existingBalance));

        // when / then
        assertThatThrownBy(() ->
                balanceService.debit(ACCOUNT_ID, CURRENCY_USD, debitAmount)
        ).isInstanceOf(InsufficientFundsException.class);

        verify(balanceRepository, never()).save(any());
        verifyNoInteractions(transactionLogService);
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException if balance entry does not exist for currency")
    void debit_noBalanceEntry_throwsException() {
        // given
        Account account = new Account().setId(ACCOUNT_ID);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, CURRENCY_USD))
                .thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
                balanceService.debit(ACCOUNT_ID, CURRENCY_USD, BigDecimal.TEN)
        ).isInstanceOf(InsufficientFundsException.class);

        verify(balanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exchange USD -> EUR: Should debit source and credit target with converted amount")
    void exchange_happyPath_shouldConvertAndSaveBoth() {
        // given
        BigDecimal sourceAmount = new BigDecimal("100.00");
        BigDecimal amountToExchange = new BigDecimal("50.00");
        BigDecimal convertedAmount = new BigDecimal("45.00");

        Account account = new Account().setId(ACCOUNT_ID);

        Balance sourceBalance = new Balance()
                .setAccount(account)
                .setCurrency("USD")
                .setAmount(sourceAmount);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, "USD"))
                .thenReturn(Optional.of(sourceBalance));

        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, "EUR"))
                .thenReturn(Optional.empty());

        when(exchangeRateService.isSupported(any(Currency.class))).thenReturn(true);
        when(exchangeRateService.convert(any(), any(), eq(amountToExchange)))
                .thenReturn(convertedAmount);

        // when
        BalanceResponse response = balanceService.exchange(ACCOUNT_ID, "USD", "EUR", amountToExchange);

        // then
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.amount()).isEqualByComparingTo(convertedAmount);

        InOrder inOrder = inOrder(balanceRepository, transactionLogService);

        inOrder.verify(balanceRepository).save(sourceBalance);
        assertThat(sourceBalance.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

        inOrder.verify(balanceRepository).save(balanceCaptor.capture());

        Balance targetBalance = balanceCaptor.getValue();
        assertThat(targetBalance.getCurrency()).isEqualTo("EUR");
        assertThat(targetBalance.getAmount()).isEqualByComparingTo(convertedAmount);
        assertThat(targetBalance.getAccount()).isEqualTo(account);

        inOrder.verify(transactionLogService).logTransaction(account, TransactionType.EXCHANGE_OUT, amountToExchange, "USD");
        inOrder.verify(transactionLogService).logTransaction(account, TransactionType.EXCHANGE_IN, convertedAmount, "EUR");
    }

    @Test
    @DisplayName("Exchange fails for same currency")
    void exchange_sameCurrency_throwsException() {
        assertThatThrownBy(() ->
                balanceService.exchange(ACCOUNT_ID, "USD", "USD", BigDecimal.TEN)
        ).isInstanceOf(CurrencyExchangeException.class)
                .hasMessageContaining("same currency");

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("Get Balance: Should return 0.00 if balance does not exist")
    void getBalance_missing_returnsZero() {
        when(balanceRepository.findBalanceByAccountIdAndCurrency(ACCOUNT_ID, "JPY"))
                .thenReturn(Optional.empty());

        BalanceResponse response = balanceService.getBalance(ACCOUNT_ID, "JPY");

        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.currency()).isEqualTo("JPY");
    }
}