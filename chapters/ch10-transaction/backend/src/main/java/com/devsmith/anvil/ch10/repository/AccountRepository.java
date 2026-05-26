package com.devsmith.anvil.ch10.repository;

import com.devsmith.anvil.ch10.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /** 비관적 락 — 데드락 시연에서 사용. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(Long id);

    /** 잔액 합계 — 트랜잭션 전후 conservation 검증용. */
    @Query("select coalesce(sum(a.balance), 0) from Account a")
    BigDecimal sumBalance();
}
