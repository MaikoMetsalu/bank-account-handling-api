package com.demo.bankaccounthandlingapi.services;

import com.demo.bankaccounthandlingapi.entities.Account;
import com.demo.bankaccounthandlingapi.entities.TransactionLog;
import com.demo.bankaccounthandlingapi.enums.TransactionType;
import com.demo.bankaccounthandlingapi.repositories.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionLogService {

    private final TransactionLogRepository transactionLogRepository;

    public TransactionLogService(TransactionLogRepository transactionLogRepository) {
        this.transactionLogRepository = transactionLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logTransaction(Account account, TransactionType transactionType, BigDecimal amount, String currency) {
        transactionLogRepository.save(new TransactionLog()
                .setAccount(account)
                .setReferenceId(java.util.UUID.randomUUID())
                .setAmount(amount)
                .setCurrency(currency)
                .setType(transactionType));
    }
}
