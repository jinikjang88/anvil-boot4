package com.devsmith.anvil.ch01.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HelloService 단위 테스트 — Spring 컨텍스트 없이 POJO 로 검증.
 * <p>given/when/then 주석 + 한글 백틱 메서드명 컨벤션을 따른다.</p>
 */
@DisplayName("HelloService")
class HelloServiceTest {

    private final HelloService helloService = new HelloService();

    @Test
    void 이름이_주어지면_그_이름으로_인사한다() {
        // given
        String name = "홍길동";

        // when
        String result = helloService.greet(name);

        // then
        assertThat(result).isEqualTo("hello, 홍길동");
    }

    @Test
    void 이름이_null이면_기본값_anvil로_인사한다() {
        // given
        String name = null;

        // when
        String result = helloService.greet(name);

        // then
        assertThat(result).isEqualTo("hello, anvil");
    }

    @Test
    void 이름이_빈문자열이면_기본값_anvil로_인사한다() {
        // given
        String name = "   ";

        // when
        String result = helloService.greet(name);

        // then
        assertThat(result).isEqualTo("hello, anvil");
    }

    @Test
    void 이름_앞뒤_공백은_제거되어_인사한다() {
        // given
        String name = "  홍길동  ";

        // when
        String result = helloService.greet(name);

        // then
        assertThat(result).isEqualTo("hello, 홍길동");
    }
}
