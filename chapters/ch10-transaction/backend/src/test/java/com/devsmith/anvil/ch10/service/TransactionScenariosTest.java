package com.devsmith.anvil.ch10.service;

import com.devsmith.anvil.ch10.PostgresContainerSpec;
import com.devsmith.anvil.ch10.domain.Account;
import com.devsmith.anvil.ch10.domain.TxLog;
import com.devsmith.anvil.ch10.repository.AccountRepository;
import com.devsmith.anvil.ch10.repository.TxLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 트랜잭션 4축 핵심 검증.
 *
 * - 격리: READ_COMMITTED 에서 송금 성공 → 잔액 합계 불변
 * - 전파: REQUIRED 로그는 롤백 시 사라짐 / REQUIRES_NEW 로그는 생존
 * - AOP: self-invocation 은 트랜잭션 없이 실행 (검증은 간접적)
 * - checked exception 은 커밋됨 / rollbackFor 로 수정하면 롤백
 */
@SpringBootTest
@DisplayName("ch10 — 트랜잭션 시나리오")
class TransactionScenariosTest extends PostgresContainerSpec {

    @Autowired TransferService transferService;
    @Autowired TxLogService txLogService;
    @Autowired AccountRepository accountRepo;
    @Autowired TxLogRepository txLogRepo;

    private Long idA, idB;

    @BeforeEach
    void seed() {
        txLogRepo.deleteAll();
        accountRepo.deleteAll();
        Account a = accountRepo.save(new Account("민지", new BigDecimal("100000")));
        Account b = accountRepo.save(new Account("준호", new BigDecimal("100000")));
        idA = a.getId();
        idB = b.getId();
    }

    // ── 격리 ──────────────────────────────────────────────────────────────

    @Test
    void READ_COMMITTED_송금_성공_후_잔액_합계_보존() {
        transferService.transferReadCommitted(idA, idB, new BigDecimal("30000"));

        assertThat(accountRepo.sumBalance()).isEqualByComparingTo("200000");
        Account a = accountRepo.findById(idA).orElseThrow();
        assertThat(a.getBalance()).isEqualByComparingTo("70000");
    }

    // ── 전파 ──────────────────────────────────────────────────────────────

    @Test
    void REQUIRED_로그는_바깥_롤백_시_함께_사라진다() {
        assertThatThrownBy(() -> transferService.propagationRequired_thenRollback())
                .isInstanceOf(RuntimeException.class);

        List<TxLog> logs = txLogService.findByScenario("propagation-required");
        assertThat(logs).as("REQUIRED 로그는 바깥 TX 롤백으로 함께 사라져야 함").isEmpty();
    }

    @Test
    void REQUIRES_NEW_로그는_바깥_롤백에도_생존한다() {
        assertThatThrownBy(() -> transferService.propagationRequiresNew_thenRollback())
                .isInstanceOf(RuntimeException.class);

        List<TxLog> logs = txLogService.findByScenario("propagation-requires-new");
        assertThat(logs).as("REQUIRES_NEW 로그는 독립 TX 라 바깥 롤백과 무관").hasSize(1);
    }

    // ── checked exception 함정 ───────────────────────────────────────────

    @Test
    void checked_exception_은_기본적으로_롤백_안_함_출금만_커밋() {
        assertThatThrownBy(() ->
                transferService.checkedExceptionTrap(idA, idB, new BigDecimal("10000")))
                .isInstanceOf(Exception.class);

        // checked exception 은 @Transactional 기본 설정으로 롤백하지 않는다 → 출금이 커밋됨.
        Account a = accountRepo.findById(idA).orElseThrow();
        assertThat(a.getBalance()).as("출금 10,000원이 커밋됐어야 함")
                .isEqualByComparingTo("90000");
        // 잔액 합계가 깨짐 (입금은 안 됨).
        assertThat(accountRepo.sumBalance()).isEqualByComparingTo("190000");
    }

    @Test
    void rollbackFor_Exception_이면_checked_exception_도_롤백() {
        assertThatThrownBy(() ->
                transferService.checkedExceptionFixed(idA, idB, new BigDecimal("10000")))
                .isInstanceOf(Exception.class);

        Account a = accountRepo.findById(idA).orElseThrow();
        assertThat(a.getBalance()).as("rollbackFor=Exception → 출금도 롤백")
                .isEqualByComparingTo("100000");
        assertThat(accountRepo.sumBalance()).isEqualByComparingTo("200000");
    }
}
