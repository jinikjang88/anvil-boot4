package com.devsmith.anvil.ch10.repository;

import com.devsmith.anvil.ch10.domain.TxLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TxLogRepository extends JpaRepository<TxLog, Long> {
    List<TxLog> findByScenarioOrderByCreatedAtDesc(String scenario);
}
