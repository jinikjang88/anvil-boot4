package com.devsmith.anvil.ch06.validation;

import com.devsmith.anvil.ch06.domain.TransferOptions;
import com.devsmith.anvil.ch06.domain.TransferRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UrgentNotifyEmailValidator
        implements ConstraintValidator<UrgentRequiresNotifyEmail, TransferRequest> {

    @Override
    public boolean isValid(TransferRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;   // @NotNull 이 잡을 영역. validator 끼리 책임 안 겹치게.
        }
        TransferOptions options = request.options();
        if (options == null || !options.urgent()) {
            return true;
        }
        boolean hasEmail = options.notifyEmail() != null && !options.notifyEmail().isBlank();
        if (!hasEmail) {
            // 기본 violation 위치(클래스 루트) 대신 options.notifyEmail 필드로 보낸다.
            // → 클라이언트가 응답의 field path 를 보고 어디가 잘못됐는지 알 수 있음.
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("options")
                    .addPropertyNode("notifyEmail")
                    .addConstraintViolation();
        }
        return hasEmail;
    }
}
