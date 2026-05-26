package com.devsmith.anvil.ch10.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 트랜잭션 시연 이벤트 로그.
 *
 * 전파 시연에서 "이 INSERT 가 커밋됐나 롤백됐나" 를 프론트가 확인하는 용도.
 * REQUIRES_NEW 안에서 저장하면 바깥 롤백과 무관하게 커밋되는 것을 보여준다.
 */
@Entity
@Table(name = "tx_logs")
public class TxLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String scenario;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TxLog() {}

    public TxLog(String scenario, String message) {
        this.scenario = scenario;
        this.message = message;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getScenario() { return scenario; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
