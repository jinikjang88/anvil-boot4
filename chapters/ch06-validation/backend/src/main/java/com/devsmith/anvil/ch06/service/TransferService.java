package com.devsmith.anvil.ch06.service;

import com.devsmith.anvil.ch06.domain.Transfer;
import com.devsmith.anvil.ch06.domain.TransferRequest;
import com.devsmith.anvil.ch06.error.NotFoundException;
import com.devsmith.anvil.ch06.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 검증을 *통과한* 요청만 이 계층에 도달한다.
 *
 * <p>즉, 이 클래스 안에서는 형식/구문 검증을 반복하지 않는다 — DRY + 책임 분리.
 * 비즈니스 규칙 검증 (예: 잔액 부족) 은 ch10 (트랜잭션) 에서 본격적으로 다룬다.</p>
 */
@Service
public class TransferService {

    private final TransferRepository repository;
    private final Clock clock;

    public TransferService(TransferRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Transfer create(TransferRequest request) {
        Transfer transfer = new Transfer(
                UUID.randomUUID().toString(),
                request.fromAccount(),
                request.toAccount(),
                request.amount(),
                request.currency(),
                request.memo() == null ? "" : request.memo(),
                request.options().urgent(),
                request.options().notifyEmail(),
                request.options().scheduledAt(),
                Instant.now(clock)
        );
        return repository.save(transfer);
    }

    public List<Transfer> list() {
        return repository.findAll();
    }

    public Transfer get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("transfer not found: " + id));
    }
}
