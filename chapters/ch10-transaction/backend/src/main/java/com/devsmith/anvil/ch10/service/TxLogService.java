package com.devsmith.anvil.ch10.service;

import com.devsmith.anvil.ch10.domain.TxLog;
import com.devsmith.anvil.ch10.repository.TxLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 전파(Propagation) 시연용 — 별도 빈으로 분리해야 AOP 프록시가 작동한다.
 *
 * 학습 포인트:
 * - REQUIRED (기본): 바깥 트랜잭션에 합류. 바깥이 롤백하면 이 로그도 같이 롤백.
 * - REQUIRES_NEW: 새 트랜잭션. 바깥이 롤백해도 이 로그는 커밋됨.
 * - NOT_SUPPORTED: 트랜잭션 없이 실행. 이전 트랜잭션 일시 중단.
 */
@Service
public class TxLogService {

    private final TxLogRepository repository;

    public TxLogService(TxLogRepository repository) {
        this.repository = repository;
    }

    /** REQUIRED — 바깥 트랜잭션에 합류. 바깥 롤백 시 함께 롤백. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logRequired(String scenario, String message) {
        repository.save(new TxLog(scenario, message));
    }

    /** REQUIRES_NEW — 독립 트랜잭션. 바깥 롤백과 무관하게 커밋. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRequiresNew(String scenario, String message) {
        repository.save(new TxLog(scenario, message));
    }

    @Transactional(readOnly = true)
    public List<TxLog> findByScenario(String scenario) {
        return repository.findByScenarioOrderByCreatedAtDesc(scenario);
    }

    @Transactional
    public void deleteByScenario(String scenario) {
        repository.deleteAll(repository.findByScenarioOrderByCreatedAtDesc(scenario));
    }
}
