package com.devsmith.anvil.ch07.controller;

import com.devsmith.anvil.ch07.error.EmailAlreadyExistsException;
import com.devsmith.anvil.ch07.error.InvalidCredentialsException;
import com.devsmith.anvil.ch07.error.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 도메인 / 검증 예외 → RFC 7807 ProblemDetail.
 * (인증/인가 관련 예외는 Spring Security 의 EntryPoint / AccessDeniedHandler 가 별도 처리.)
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final URI VALIDATION   = URI.create("https://anvil.run/errors/validation");
    private static final URI CONFLICT     = URI.create("https://anvil.run/errors/conflict");
    private static final URI UNAUTHORIZED = URI.create("https://anvil.run/errors/unauthorized");
    private static final URI NOT_FOUND    = URI.create("https://anvil.run/errors/not-found");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, Object>> errors = new ArrayList<>();
        for (FieldError f : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("field", f.getField());
            entry.put("rejectedValue", f.getRejectedValue());
            entry.put("code", f.getCode());
            entry.put("message", f.getDefaultMessage());
            errors.add(entry);
        }
        return problem(HttpStatus.BAD_REQUEST, VALIDATION, "Validation Failed",
                "요청 본문이 검증 규칙을 위반했습니다", req, errors);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> conflict(EmailAlreadyExistsException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, CONFLICT, "Conflict", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> credentials(InvalidCredentialsException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, UNAUTHORIZED, "Unauthorized", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(NotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, "Not Found", ex.getMessage(), req, List.of());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, URI type, String title,
                                                         String detail, HttpServletRequest req,
                                                         List<Map<String, Object>> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(type);
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        if (!errors.isEmpty()) pd.setProperty("errors", errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
