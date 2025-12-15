package com.demo.bankaccounthandlingapi.entities;

import com.demo.bankaccounthandlingapi.enums.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_log")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public TransactionLog setId(Long id) {
        this.id = id;
        return this;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionLog setAccount(Account account) {
        this.account = account;
        return this;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public TransactionLog setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionLog setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionLog setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionLog setType(TransactionType type) {
        this.type = type;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public TransactionLog setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}