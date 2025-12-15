package com.demo.bankaccounthandlingapi.repositories;

import com.demo.bankaccounthandlingapi.entities.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
}
