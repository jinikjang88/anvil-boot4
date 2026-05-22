package com.devsmith.anvil.ch01.controller;

import com.devsmith.anvil.ch01.service.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ch01 의 단일 엔드포인트:
 * <pre>
 *   GET /api/hello              → { "message": "hello, anvil" }
 *   GET /api/hello?name=홍길동  → { "message": "hello, 홍길동" }
 * </pre>
 *
 * <p>생성자 주입을 사용해 final 필드로 의존성을 고정한다.
 * — 테스트에서 mock 주입이 명시적이고, 순환 참조를 컴파일 시점에 차단한다 (ch03 복선).</p>
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    /**
     * 인사 메시지를 JSON 으로 반환한다.
     *
     * <p>{@code Map.of(...)} 를 반환해 별도 DTO 없이 학습 흐름을 단순화했다.
     * 실무에서는 ch05 에서 record DTO 로 전환한다.</p>
     */
    @GetMapping("/hello")
    public HelloResponse hello(@RequestParam(required = false) String name) {
        return new HelloResponse(helloService.greet(name));
    }

    /**
     * Java 25 record — 불변 응답 DTO.
     * Boot 4 에서 record 직렬화는 별도 설정 없이 동작한다.
     */
    public record HelloResponse(String message) { }
}
