package com.devsmith.anvil.ch08.controller;

import com.devsmith.anvil.ch08.error.PostNotFoundException;
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
 * RFC 7807 ProblemDetail 기반 예외 핸들러. ch07 패턴 차용.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final URI VALIDATION = URI.create("https://anvil.run/errors/validation");
    private static final URI NOT_FOUND  = URI.create("https://anvil.run/errors/not-found");

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

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(PostNotFoundException ex, HttpServletRequest req) {
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
