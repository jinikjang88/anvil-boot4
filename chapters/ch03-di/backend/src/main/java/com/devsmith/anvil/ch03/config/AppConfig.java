package com.devsmith.anvil.ch03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 애플리케이션 차원의 빈 설정.
 *
 * <p>학습 포인트 — {@code @Configuration + @Bean} 이 필요한 대표 케이스:
 * <ul>
 *   <li>해당 클래스를 우리가 <i>소유하지 않을 때</i> (예: {@link Clock} 은 JDK 클래스라
 *       {@code @Component} 를 붙일 수 없다)</li>
 *   <li>빈 생성 과정에 <i>로직</i> 이 들어갈 때 (조건 분기, 외부 설정 읽기 등)</li>
 *   <li>같은 타입의 빈을 <i>여러 개 다른 설정</i> 으로 만들어야 할 때</li>
 * </ul>
 * </p>
 *
 * <p>반면 우리가 만든 일반 서비스/리포지토리는 {@code @Component}/{@code @Service}/{@code @Repository}
 * 로 자동 스캔되게 두는 편이 훨씬 간결하다 (선언 위치가 클래스 자체에 있어 발견하기 쉬움).</p>
 */
@Configuration
public class AppConfig {

    /**
     * 시스템 기본 시간대 기반 Clock 빈.
     *
     * <p>테스트에선 {@code Clock.fixed(...)} 로 대체해 시간을 결정적으로 만들 수 있다.
     * 서비스 코드에서 {@code Instant.now()} 대신 {@code Instant.now(clock)} 을 쓰는 이유.</p>
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
