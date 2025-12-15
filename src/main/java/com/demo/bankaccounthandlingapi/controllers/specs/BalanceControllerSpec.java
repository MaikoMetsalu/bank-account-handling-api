package com.demo.bankaccounthandlingapi.controllers.specs;

import com.demo.bankaccounthandlingapi.dtos.BalanceResponse;
import com.demo.bankaccounthandlingapi.dtos.DepositRequest;
import com.demo.bankaccounthandlingapi.dtos.ExchangeRequest;
import com.demo.bankaccounthandlingapi.dtos.WithdrawRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Balance Management", description = "Operations for deposits, withdrawals, and currency exchange")
public interface BalanceControllerSpec {

    @Operation(summary = "Deposit funds", description = "Add money to a specific currency balance for the account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit successful",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input (e.g. negative amount)", content = @Content)
    })
    BalanceResponse deposit(
            @Parameter(description = "The unique ID of the account", example = "1001", required = true)
            @PathVariable Long accountId,

            @RequestBody(description = "Deposit details including currency and amount", required = true)
            DepositRequest depositRequest
    );

    @Operation(summary = "Withdraw funds", description = "Debit money from the account. Performs an external security check before processing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Withdrawal successful",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Insufficient funds or invalid input", content = @Content),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content),
            @ApiResponse(responseCode = "503", description = "External system unavailable", content = @Content)
    })
    BalanceResponse withdraw(
            @Parameter(description = "The unique ID of the account", example = "1001", required = true)
            @PathVariable Long accountId,

            @RequestBody(description = "Withdrawal details", required = true)
            WithdrawRequest withdrawRequest
    );

    @Operation(summary = "Get Balance", description = "Retrieve the current balance for a specific currency.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    })
    BalanceResponse getBalance(
            @Parameter(description = "The unique ID of the account", example = "1001", required = true)
            @PathVariable Long accountId,

            @Parameter(description = "3-letter Currency Code (e.g. USD, EUR)", example = "USD", required = true)
            @RequestParam String currency
    );

    @Operation(summary = "Exchange Currency", description = "Convert funds from one currency to another using fixed exchange rates.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange successful",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Insufficient source funds or unsupported currency pair", content = @Content),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    })
    BalanceResponse exchange(
            @Parameter(description = "The unique ID of the account", example = "1001", required = true)
            @PathVariable Long accountId,

            @RequestBody(description = "Exchange details", required = true)
            ExchangeRequest exchangeRequest
    );
}