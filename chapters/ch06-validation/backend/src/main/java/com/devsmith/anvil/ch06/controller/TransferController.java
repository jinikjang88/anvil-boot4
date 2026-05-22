package com.devsmith.anvil.ch06.controller;

import com.devsmith.anvil.ch06.domain.Transfer;
import com.devsmith.anvil.ch06.domain.TransferRequest;
import com.devsmith.anvil.ch06.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 송금 요청 컨트롤러.
 *
 * <p>{@code @Valid} 가 본문 검증을 트리거 — 실패하면 Spring 이
 * {@code MethodArgumentNotValidException} 을 던지고 {@link ValidationExceptionHandler}
 * 가 RFC 7807 Problem Details 형태로 응답을 만든다.</p>
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Transfer> create(@Valid @RequestBody TransferRequest request) {
        Transfer created = service.create(request);
        URI location = URI.create("/api/v1/transfers/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<Transfer> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Transfer get(@PathVariable String id) {
        return service.get(id);
    }
}
