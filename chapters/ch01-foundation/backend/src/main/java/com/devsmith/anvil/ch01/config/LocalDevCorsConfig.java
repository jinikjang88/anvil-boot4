package com.devsmith.anvil.ch01.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 개발용 CORS 허용.
 *
 * <p>frontend/index.html 은 보통 {@code python -m http.server 5173} 같은 별도 포트에서
 * 서빙되므로, 백엔드(8080) 를 호출하려면 브라우저 CORS 정책을 통과시켜야 한다.
 * 이 빈은 {@code spring.profiles.active=local} 일 때만 활성화되어
 * dev/prod 환경에서는 자동으로 꺼진다.</p>
 *
 * <p>실 운영용 CORS / CSRF / 인증 헤더는 <b>ch07-security</b> 에서 본격적으로 다룬다.
 * ch01 은 데모가 동작한다는 사실 자체가 목적이므로 가장 단순한 형태로 유지한다.</p>
 */
@Configuration
@Profile("local")
public class LocalDevCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET");
    }
}
