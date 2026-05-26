package com.devsmith.anvil.ch10.service;

import com.devsmith.anvil.ch10.domain.Account;
import com.devsmith.anvil.ch10.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 송금 서비스 — 트랜잭션 격리/전파/AOP 함정 시연의 메인.
 *
 * 학습 의도: 같은 transfer 로직을 격리 수준별로 호출해 동작 차이를 본다.
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TxLogService txLogService;

    public TransferService(AccountRepository accountRepository, TxLogService txLogService) {
        this.accountRepository = accountRepository;
        this.txLogService = txLogService;
    }

    // ── 격리 수준별 송금 ────────────────────────────────────────────────

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String transferReadCommitted(Long fromId, Long toId, BigDecimal amount) {
        return doTransfer(fromId, toId, amount, "READ_COMMITTED");
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String transferRepeatableRead(Long fromId, Long toId, BigDecimal amount) {
        return doTransfer(fromId, toId, amount, "REPEATABLE_READ");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String transferSerializable(Long fromId, Long toId, BigDecimal amount) {
        return doTransfer(fromId, toId, amount, "SERIALIZABLE");
    }

    private String doTransfer(Long fromId, Long toId, BigDecimal amount, String level) {
        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 없음: " + fromId));
        Account to = accountRepository.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 없음: " + toId));

        from.withdraw(amount);
        to.deposit(amount);

        return level + " 송금 완료: " + from.getOwner() + " → " + to.getOwner() + " " + amount + "원";
    }

    // ── 전파(Propagation) 시연 ──────────────────────────────────────────

    /**
     * REQUIRED 로그 + 롤백 → 로그도 롤백됨.
     * 바깥 트랜잭션이 의도적으로 예외를 던져 롤백.
     */
    @Transactional
    public void propagationRequired_thenRollback() {
        txLogService.logRequired("propagation-required", "REQUIRED 로그 — 바깥과 같은 TX");
        throw new RuntimeException("의도적 롤백 — REQUIRED 로그도 함께 사라져야 함");
    }

    /**
     * REQUIRES_NEW 로그 + 바깥 롤백 → 로그는 살아남음.
     */
    @Transactional
    public void propagationRequiresNew_thenRollback() {
        txLogService.logRequiresNew("propagation-requires-new", "REQUIRES_NEW 로그 — 독립 TX");
        throw new RuntimeException("의도적 롤백 — REQUIRES_NEW 로그는 살아남아야 함");
    }

    // ── AOP 함정 시연 ────────────────────────────────────────────────────

    /**
     * self-invocation 함정: 같은 클래스 안에서 @Transactional 메서드를 호출하면
     * AOP 프록시를 거치지 않으므로 트랜잭션이 안 걸린다.
     *
     * 이 메서드는 @Transactional 이 **없다** — 내부에서 internalTransfer() 를 호출하지만,
     * self-invocation 이라 프록시를 타지 않아 트랜잭션 없이 실행된다.
     */
    public String selfInvocationTrap(Long fromId, Long toId, BigDecimal amount) {
        // 이 호출은 this.internalTransfer() — 프록시가 아닌 실제 객체의 메서드.
        // → @Transactional 무시됨.
        return internalTransfer(fromId, toId, amount);
    }

    @Transactional
    public String internalTransfer(Long fromId, Long toId, BigDecimal amount) {
        return doTransfer(fromId, toId, amount, "SELF_INVOCATION");
    }

    /**
     * checked exception 함정: @Transactional 은 기본적으로 RuntimeException 만 롤백.
     * checked exception (Exception) 을 던지면 **커밋**된다.
     */
    @Transactional
    public void checkedExceptionTrap(Long fromId, Long toId, BigDecimal amount) throws Exception {
        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 없음: " + fromId));
        from.withdraw(amount);
        // 출금은 했지만 입금 전에 checked exception → 커밋됨!
        throw new Exception("checked exception — @Transactional 기본은 롤백 안 함");
    }

    /**
     * checked exception + rollbackFor 수정 — 올바른 패턴.
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkedExceptionFixed(Long fromId, Long toId, BigDecimal amount) throws Exception {
        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 없음: " + fromId));
        from.withdraw(amount);
        throw new Exception("checked exception — rollbackFor=Exception.class 라 롤백됨");
    }

    // ── 데드락 시연 ──────────────────────────────────────────────────────

    /**
     * 비관적 락으로 계좌를 잡는 메서드. 두 스레드가 순서를 바꿔 호출하면 데드락.
     */
    @Transactional(timeout = 5)
    public String pessimisticTransfer(Long firstLockId, Long secondLockId, BigDecimal amount) {
        Account first = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 없음: " + firstLockId));
        // 의도적 지연 — 상대 스레드가 반대 순서로 락을 잡을 시간을 줌.
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Account second = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new IllegalArgumentException("계좌 없음: " + secondLockId));

        first.withdraw(amount);
        second.deposit(amount);

        return "비관적 락 송금 완료: " + first.getOwner() + " → " + second.getOwner();
    }

    // ── 조회 ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BigDecimal sumBalance() {
        return accountRepository.sumBalance();
    }
}
