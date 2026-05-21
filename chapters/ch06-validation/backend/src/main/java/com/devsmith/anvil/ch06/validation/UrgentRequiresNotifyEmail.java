package com.devsmith.anvil.ch06.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cross-field 제약 — {@code options.urgent == true} 면 {@code options.notifyEmail} 이 필수.
 *
 * <p>단일 필드 어노테이션({@code @NotBlank} 등) 만으로는 "다른 필드의 값에 따라 이 필드의
 * 요구가 달라지는" 규칙을 표현할 수 없다 — 이때가 custom validator 의 자리.</p>
 *
 * <p>대상은 {@code TYPE} 이므로 record 클래스 위에 붙는다 (record 도 결국 class).</p>
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrgentNotifyEmailValidator.class)
public @interface UrgentRequiresNotifyEmail {

    String message() default "urgent=true 면 notifyEmail 이 필수입니다";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
