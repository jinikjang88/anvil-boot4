package com.devsmith.anvil.ch06.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validator API 를 직접 사용한 단위 테스트 — Spring 컨텍스트 0.
 * <p>각 필드 위반이 expected path 와 code 로 잡히는지 검증.</p>
 */
@DisplayName("TransferRequest Bean Validation")
class TransferRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void boot() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void shutdown() {
        factory.close();
    }

    @Test
    void 정상_요청은_위반_0건() {
        Set<ConstraintViolation<TransferRequest>> violations = validator.validate(sample());
        assertThat(violations).isEmpty();
    }

    @Test
    void amount가_100미만이면_DecimalMin_위반() {
        TransferRequest req = withAmount(new BigDecimal("99"));

        var violations = validator.validate(req);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
        assertThat(violations)
                .anySatisfy(v -> assertThat(v.getMessage()).contains("100 이상"));
    }

    @Test
    void amount가_음수면_DecimalMin_위반() {
        var violations = validator.validate(withAmount(new BigDecimal("-1000")));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void currency가_3자리_대문자_아니면_Pattern_위반() {
        TransferRequest req = withCurrency("won");

        var violations = validator.validate(req);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("currency");
    }

    @Test
    void notifyEmail이_잘못된_형식이면_options_notifyEmail_위반() {
        TransferRequest req = withOptions(new TransferOptions(false, "not-an-email", null));

        var violations = validator.validate(req);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("options.notifyEmail");
    }

    @Test
    void scheduledAt이_과거면_FutureOrPresent_위반() {
        TransferRequest req = withOptions(new TransferOptions(false, null, LocalDate.now().minusDays(1)));

        var violations = validator.validate(req);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("options.scheduledAt");
    }

    @Test
    void urgent_true인데_notifyEmail이_null이면_cross_field_위반() {
        TransferRequest req = withOptions(new TransferOptions(true, null, null));

        var violations = validator.validate(req);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("options.notifyEmail");
        assertThat(violations)
                .anySatisfy(v -> assertThat(v.getMessage()).contains("notifyEmail"));
    }

    @Test
    void urgent_true_여도_notifyEmail이_있으면_통과() {
        TransferRequest req = withOptions(new TransferOptions(true, "a@b.com", null));

        var violations = validator.validate(req);

        assertThat(violations).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private static TransferRequest sample() {
        return new TransferRequest(
                "ACC-001", "ACC-002",
                new BigDecimal("10000"), "KRW",
                "정상 송금",
                new TransferOptions(false, null, null)
        );
    }

    private static TransferRequest withAmount(BigDecimal amount) {
        TransferRequest s = sample();
        return new TransferRequest(s.fromAccount(), s.toAccount(), amount, s.currency(), s.memo(), s.options());
    }

    private static TransferRequest withCurrency(String currency) {
        TransferRequest s = sample();
        return new TransferRequest(s.fromAccount(), s.toAccount(), s.amount(), currency, s.memo(), s.options());
    }

    private static TransferRequest withOptions(TransferOptions options) {
        TransferRequest s = sample();
        return new TransferRequest(s.fromAccount(), s.toAccount(), s.amount(), s.currency(), s.memo(), options);
    }
}
