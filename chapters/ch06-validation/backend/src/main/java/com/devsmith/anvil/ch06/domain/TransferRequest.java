package com.devsmith.anvil.ch06.domain;

import com.devsmith.anvil.ch06.validation.UrgentRequiresNotifyEmail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 송금 요청 — 컨트롤러가 받는 본문 DTO.
 *
 * <p>학습 포인트: 단일 필드 어노테이션과 cross-field 어노테이션이 한 record 위에서
 * 자연스럽게 공존한다. compact constructor 검증(ch02/ch05) 은 *마지막 방어선* 이고,
 * 입구에서의 친절한 응답은 Bean Validation 의 몫.</p>
 */
@UrgentRequiresNotifyEmail
public record TransferRequest(

        @NotBlank(message = "fromAccount 는 필수")
        @Size(max = 20, message = "fromAccount 는 20자 이내")
        String fromAccount,

        @NotBlank(message = "toAccount 는 필수")
        @Size(max = 20, message = "toAccount 는 20자 이내")
        String toAccount,

        @NotNull(message = "amount 는 필수")
        @DecimalMin(value = "100",      message = "amount 는 100 이상이어야 합니다")
        @DecimalMax(value = "10000000", message = "amount 는 10,000,000 이하여야 합니다")
        BigDecimal amount,

        @NotBlank(message = "currency 는 필수")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency 는 ISO 4217 3자리 대문자 (예: KRW)")
        String currency,

        @Size(max = 200, message = "memo 는 200자 이내")
        String memo,

        @NotNull(message = "options 는 필수")
        @Valid
        TransferOptions options
) { }
