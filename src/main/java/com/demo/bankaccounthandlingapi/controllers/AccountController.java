package com.demo.bankaccounthandlingapi.controllers;

import com.demo.bankaccounthandlingapi.entities.Account;
import com.demo.bankaccounthandlingapi.repositories.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Dummy endpoint to create an account for testing purposes
    @PostMapping
    public ResponseEntity<Account> createAccount() {
        Account newAccount = new Account();
        Account savedAccount = accountRepository.save(newAccount);
        return ResponseEntity.ok(savedAccount);
    }
}
