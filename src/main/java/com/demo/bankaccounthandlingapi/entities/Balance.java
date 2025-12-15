package com.demo.bankaccounthandlingapi.entities;

import com.demo.bankaccounthandlingapi.exceptions.IllegalBalanceUpdateException;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "balance", uniqueConstraints = {
        @UniqueConstraint(name = "uq_account_currency", columnNames = {"account_id", "currency"})
})
public class Balance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version;

    public Long getId() {
        return id;
    }

    public Balance setId(Long id) {
        this.id = id;
        return this;
    }

    public Account getAccount() {
        return account;
    }

    public Balance setAccount(Account account) {
        this.account = account;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Balance setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Balance setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public Long getVersion() {
        return version;
    }

    public Balance setVersion(Long version) {
        this.version = version;
        return this;
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalBalanceUpdateException(this.account.getId());
        }
        this.amount = this.amount.add(amount);
    }
}