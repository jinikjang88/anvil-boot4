package com.devsmith.anvil.ch05.controller;

import com.devsmith.anvil.ch05.domain.Order;
import com.devsmith.anvil.ch05.domain.OrderStatus;
import com.devsmith.anvil.ch05.repository.OrderRepository;
import com.devsmith.anvil.ch05.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST 정석 매핑.
 *
 * <pre>
 *   POST   /api/v1/orders                 → 201 Created + Location
 *   GET    /api/v1/orders?status=&page=&size=  → 200
 *   GET    /api/v1/orders/{id}            → 200 / 404
 *   PATCH  /api/v1/orders/{id}            → 200 / 404
 *   PUT    /api/v1/orders/{id}/status     → 200 / 404 / 409
 *   DELETE /api/v1/orders/{id}            → 204 / 404
 * </pre>
 *
 * <p>URL 버저닝은 {@code /api/v1/} 로 prefix. v2 호환성 전략은 README 의
 * "더 깊이" 섹션에서 다룬다 — 본 챕터에는 v2 더미 코드를 두지 않는다 (MUST 원칙).</p>
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderRequests.Create request) {
        Order created = service.create(request.customer(), request.items(), request.memo());
        URI location = URI.create("/api/v1/orders/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public OrderRepository.Page<Order> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable String id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public Order patch(@PathVariable String id, @RequestBody OrderRequests.PatchMemo request) {
        return service.updateMemo(id, request.memo());
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable String id, @RequestBody OrderRequests.TransitionStatus request) {
        return service.transition(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
