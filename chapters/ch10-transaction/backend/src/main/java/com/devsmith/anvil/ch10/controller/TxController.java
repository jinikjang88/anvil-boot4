package com.devsmith.anvil.ch10.controller;

import com.devsmith.anvil.ch10.controller.TxDtos.AccountView;
import com.devsmith.anvil.ch10.controller.TxDtos.TransferRequest;
import com.devsmith.anvil.ch10.controller.TxDtos.TxLogView;
import com.devsmith.anvil.ch10.controller.TxDtos.TxResult;
import com.devsmith.anvil.ch10.domain.Account;
import com.devsmith.anvil.ch10.repository.AccountRepository;
import com.devsmith.anvil.ch10.service.TransferService;
import com.devsmith.anvil.ch10.service.TxLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 트랜잭션 4축 시연 API.
 *
 *  POST /api/v1/tx/isolation/read-committed
 *  POST /api/v1/tx/isolation/repeatable-read
 *  POST /api/v1/tx/isolation/serializable
 *  POST /api/v1/tx/propagation/required-rollback
 *  POST /api/v1/tx/propagation/requires-new-rollback
 *  POST /api/v1/tx/trap/self-invocation
 *  POST /api/v1/tx/trap/checked-exception
 *  POST /api/v1/tx/trap/checked-exception-fixed
 *  POST /api/v1/tx/deadlock
 *  POST /api/v1/tx/reset
 *  GET  /api/v1/tx/accounts
 */
@RestController
@RequestMapping("/api/v1/tx")
public class TxController {

    private final TransferService transferService;
    private final TxLogService txLogService;
    private final AccountRepository accountRepository;

    public TxController(TransferService transferService,
                        TxLogService txLogService,
                        AccountRepository accountRepository) {
        this.transferService = transferService;
        this.txLogService = txLogService;
        this.accountRepository = accountRepository;
    }

    // ── 격리 ──────────────────────────────────────────────────────────────

    @PostMapping("/isolation/read-committed")
    public TxResult isolationReadCommitted(@RequestBody TransferRequest req) {
        return runScenario("READ_COMMITTED", () ->
                transferService.transferReadCommitted(req.fromId(), req.toId(), req.amount()));
    }

    @PostMapping("/isolation/repeatable-read")
    public TxResult isolationRepeatableRead(@RequestBody TransferRequest req) {
        return runScenario("REPEATABLE_READ", () ->
                transferService.transferRepeatableRead(req.fromId(), req.toId(), req.amount()));
    }

    @PostMapping("/isolation/serializable")
    public TxResult isolationSerializable(@RequestBody TransferRequest req) {
        return runScenario("SERIALIZABLE", () ->
                transferService.transferSerializable(req.fromId(), req.toId(), req.amount()));
    }

    // ── 전파 ──────────────────────────────────────────────────────────────

    @PostMapping("/propagation/required-rollback")
    public TxResult propagationRequired() {
        txLogService.deleteByScenario("propagation-required");
        return runScenario("PROPAGATION_REQUIRED", () -> {
            transferService.propagationRequired_thenRollback();
            return "should not reach";
        }, "propagation-required");
    }

    @PostMapping("/propagation/requires-new-rollback")
    public TxResult propagationRequiresNew() {
        txLogService.deleteByScenario("propagation-requires-new");
        return runScenario("PROPAGATION_REQUIRES_NEW", () -> {
            transferService.propagationRequiresNew_thenRollback();
            return "should not reach";
        }, "propagation-requires-new");
    }

    // ── AOP 함정 ─────────────────────────────────────────────────────────

    @PostMapping("/trap/self-invocation")
    public TxResult trapSelfInvocation(@RequestBody TransferRequest req) {
        return runScenario("SELF_INVOCATION", () ->
                transferService.selfInvocationTrap(req.fromId(), req.toId(), req.amount()));
    }

    @PostMapping("/trap/checked-exception")
    public TxResult trapCheckedException(@RequestBody TransferRequest req) {
        return runScenario("CHECKED_EXCEPTION", () -> {
            transferService.checkedExceptionTrap(req.fromId(), req.toId(), req.amount());
            return "should not reach";
        });
    }

    @PostMapping("/trap/checked-exception-fixed")
    public TxResult trapCheckedExceptionFixed(@RequestBody TransferRequest req) {
        return runScenario("CHECKED_EXCEPTION_FIXED", () -> {
            transferService.checkedExceptionFixed(req.fromId(), req.toId(), req.amount());
            return "should not reach";
        });
    }

    // ── 데드락 ────────────────────────────────────────────────────────────

    @PostMapping("/deadlock")
    public TxResult deadlock(@RequestBody TransferRequest req) {
        long start = System.nanoTime();
        // 두 스레드가 순서를 바꿔 비관적 락을 잡는다 → 데드락.
        try (ExecutorService exec = Executors.newFixedThreadPool(2)) {
            BigDecimal amount = req.amount();
            CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() ->
                    transferService.pessimisticTransfer(req.fromId(), req.toId(), amount), exec);
            CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() ->
                    transferService.pessimisticTransfer(req.toId(), req.fromId(), amount), exec);

            try {
                String r1 = f1.join();
                String r2 = f2.join();
                return result("DEADLOCK", true, r1 + " / " + r2, start, null);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                return result("DEADLOCK", false,
                        "데드락 또는 타임아웃: " + cause.getClass().getSimpleName() + " — " + cause.getMessage(),
                        start, null);
            }
        }
    }

    // ── 리셋 / 조회 ─────────────────────────────────────────────────────

    @PostMapping("/reset")
    public TxResult reset() {
        long start = System.nanoTime();
        accountRepository.deleteAll();
        Account a = accountRepository.save(new Account("민지", new BigDecimal("100000")));
        Account b = accountRepository.save(new Account("준호", new BigDecimal("100000")));
        Account c = accountRepository.save(new Account("지훈", new BigDecimal("100000")));
        return result("RESET", true, "3 계좌 초기화 (각 100,000원)", start, null);
    }

    @GetMapping("/accounts")
    public TxResult accounts() {
        long start = System.nanoTime();
        return result("ACCOUNTS", true, "현재 상태", start, null);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface ThrowingSupplier { String get() throws Exception; }

    private TxResult runScenario(String scenario, ThrowingSupplier action) {
        return runScenario(scenario, action, null);
    }

    private TxResult runScenario(String scenario, ThrowingSupplier action, String logScenario) {
        long start = System.nanoTime();
        try {
            String msg = action.get();
            return result(scenario, true, msg, start, logScenario);
        } catch (Exception e) {
            return result(scenario, false,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), start, logScenario);
        }
    }

    private TxResult result(String scenario, boolean success, String message, long startNanos, String logScenario) {
        long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
        List<AccountView> accounts = accountRepository.findAll().stream()
                .map(a -> new AccountView(a.getId(), a.getOwner(), a.getBalance(), a.getVersion()))
                .toList();
        BigDecimal total = accountRepository.sumBalance();
        List<TxLogView> logs = logScenario != null
                ? txLogService.findByScenario(logScenario).stream()
                    .map(l -> new TxLogView(l.getId(), l.getScenario(), l.getMessage(), l.getCreatedAt()))
                    .toList()
                : List.of();
        return new TxResult(scenario, success, message, accounts, total, logs, elapsed);
    }
}
