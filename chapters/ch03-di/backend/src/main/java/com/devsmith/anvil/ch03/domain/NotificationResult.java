package com.devsmith.anvil.ch03.domain;

import java.time.Instant;

public record NotificationResult(
        String recipient,
        String channel,
        String body,
        Instant sentAt,
        boolean delivered
) { }
