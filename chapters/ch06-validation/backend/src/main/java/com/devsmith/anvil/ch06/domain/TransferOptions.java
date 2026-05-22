package com.devsmith.anvil.ch06.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

/**
 * 송금 옵션. {@code TransferRequest} 안에 nested 로 들어가며, 부모의 {@code @Valid} 가
 * 이 record 의 필드까지 재귀적으로 검증한다.
 */
public record TransferOptions(

        boolean urgent,

        @Email(message = "올바른 이메일 형식이어야 합니다")
        String notifyEmail,

        @FutureOrPresent(message = "scheduledAt 은 오늘 이후여야 합니다")
        LocalDate scheduledAt
) { }
