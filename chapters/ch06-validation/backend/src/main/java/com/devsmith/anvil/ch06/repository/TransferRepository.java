package com.devsmith.anvil.ch06.repository;

import com.devsmith.anvil.ch06.domain.Transfer;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory 저장소 — ch08 까지 JPA 미사용. */
@Repository
public class TransferRepository {

    private final Map<String, Transfer> store = new ConcurrentHashMap<>();

    public Transfer save(Transfer transfer) {
        store.put(transfer.id(), transfer);
        return transfer;
    }

    public Optional<Transfer> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Transfer> findAll() {
        Collection<Transfer> values = store.values();
        return values.stream()
                .sorted(Comparator.comparing(Transfer::createdAt).reversed())
                .toList();
    }
}
