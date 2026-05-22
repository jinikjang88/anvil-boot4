package com.devsmith.anvil.ch06.controller;

import com.devsmith.anvil.ch06.error.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RFC 7807 Problem Details 응답으로 검증/404 에러를 표준화한다.
 *
 * <pre>
 *   Content-Type: application/problem+json
 *   {
 *     "type":     "https://anvil.run/errors/validation",
 *     "title":    "Validation Failed",
 *     "status":   400,
 *     "detail":   "요청 본문이 검증 규칙을 위반했습니다",
 *     "instance": "/api/v1/transfers",
 *     "errors":   [ { field, rejectedValue, code, message }, ... ]
 *   }
 * </pre>
 *
 * <p>{@code errors[]} 는 RFC 7807 의 *확장 속성* 이다 — 표준이 권장하는 패턴
 * ("문제마다 필요한 정보를 자유롭게 덧붙여라"). 클라이언트가 field path 로 UI 마킹을 할 수 있다.</p>
 */
@RestControllerAdvice
public class ValidationExceptionHandler {

    private static final URI VALIDATION_TYPE = URI.create("https://anvil.run/errors/validation");
    private static final URI NOT_FOUND_TYPE  = URI.create("https://anvil.run/errors/not-found");

    /** {@code @Valid @RequestBody} 본문 검증 실패. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError(fieldError));
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            errors.add(globalError(globalError));
        }
        return problem(HttpStatus.BAD_REQUEST,
                VALIDATION_TYPE,
                "Validation Failed",
                "요청 본문이 검증 규칙을 위반했습니다",
                request.getRequestURI(),
                errors);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex,
                                                        HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND,
                NOT_FOUND_TYPE,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                List.of());
    }

    // ─── helpers ─────────────────────────────────────────────────────

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status,
                                                         URI type,
                                                         String title,
                                                         String detail,
                                                         String instance,
                                                         List<Map<String, Object>> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(type);
        pd.setTitle(title);
        pd.setInstance(URI.create(instance));
        if (!errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private static Map<String, Object> fieldError(FieldError fieldError) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("field", fieldError.getField());
        entry.put("rejectedValue", fieldError.getRejectedValue());
        entry.put("code", fieldError.getCode());
        entry.put("message", fieldError.getDefaultMessage());
        return entry;
    }

    private static Map<String, Object> globalError(ObjectError globalError) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("field", globalError.getObjectName());   // class-level constraint
        entry.put("rejectedValue", null);
        entry.put("code", globalError.getCode());
        entry.put("message", globalError.getDefaultMessage());
        return entry;
    }
}
