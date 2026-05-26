package com.devsmith.anvil.ch10.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class TxDtos {

    private TxDtos() {}

    public record TransferRequest(Long fromId, Long toId, BigDecimal amount) {}

    public record AccountView(Long id, String owner, BigDecimal balance, Long version) {}

    public record TxLogView(Long id, String scenario, String message, Instant createdAt) {}

    public record TxResult(
            String scenario,
            boolean success,
            String message,
            List<AccountView> accounts,
            BigDecimal totalBalance,
            List<TxLogView> logs,
            long elapsedMillis
    ) {}
}
