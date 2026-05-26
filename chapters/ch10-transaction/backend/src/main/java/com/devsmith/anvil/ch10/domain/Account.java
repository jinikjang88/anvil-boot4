package com.devsmith.anvil.ch10.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 은행 계좌 엔티티.
 *
 * 학습 포인트:
 * - @Version — 낙관적 락. 두 트랜잭션이 동시에 같은 row 를 수정하면 하나가
 *   OptimisticLockException 으로 실패. Lost Update 방지의 정석.
 * - BigDecimal — 금액은 절대 double/float 금지 (부동소수점 오차).
 * - 잔액 합계 불변(conservation) — 트랜잭션 전후 모든 계좌 잔액 합이 동일해야.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    private Long version;

    protected Account() {}

    public Account(String owner, BigDecimal balance) {
        this.owner = owner;
        this.balance = balance;
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("출금 금액은 양수여야 합니다");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("잔액 부족: " + balance + " < " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("입금 금액은 양수여야 합니다");
        }
        this.balance = this.balance.add(amount);
    }

    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(getClass()); }

    @Override
    public String toString() {
        return "Account{id=" + id + ", owner='" + owner + "', balance=" + balance + "}";
    }
}
