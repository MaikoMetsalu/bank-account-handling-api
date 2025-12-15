package com.demo.bankaccounthandlingapi.unittest;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.exceptions.ExternalSystemException;
import com.demo.bankaccounthandlingapi.external.ExternalLoggingService;
import com.demo.bankaccounthandlingapi.services.AccountOperationService;
import com.demo.bankaccounthandlingapi.services.BalanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountOperationServiceTest {

    @Mock
    private ExternalLoggingService externalLoggingService;

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private AccountOperationService accountOperationService;

    private static final Long ACCOUNT_ID = 1L;
    private static final String CURRENCY = "EUR";
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    @Test
    @DisplayName("Should call external log before debiting account")
    void debitAccount_shouldCallLogBeforeDebit() {
        // given
        BalanceResponse expectedResponse = new BalanceResponse(CURRENCY, new BigDecimal("900.00"));
        when(balanceService.debit(ACCOUNT_ID, CURRENCY, AMOUNT)).thenReturn(expectedResponse);

        // when
        BalanceResponse result = accountOperationService.debitAccount(ACCOUNT_ID, CURRENCY, AMOUNT);

        // then
        assertThat(result).isEqualTo(expectedResponse);

        InOrder inOrder = inOrder(externalLoggingService, balanceService);

        inOrder.verify(externalLoggingService).sendLog();
        inOrder.verify(balanceService).debit(ACCOUNT_ID, CURRENCY, AMOUNT);
    }

    @Test
    @DisplayName("Should abort transaction if external log fails")
    void debitAccount_shouldAbortIfExternalLogFails() {
        // given
        doThrow(new ExternalSystemException("Service unavailable"))
                .when(externalLoggingService).sendLog();

        // when / then
        assertThatThrownBy(() ->
                accountOperationService.debitAccount(ACCOUNT_ID, CURRENCY, AMOUNT)
        ).isInstanceOf(ExternalSystemException.class);

        verifyNoInteractions(balanceService);
    }
}