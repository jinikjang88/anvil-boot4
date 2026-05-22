package com.devsmith.anvil.ch02.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 개발용 CORS — frontend(5173) → backend(8080) 호출 허용.
 * {@code local} 프로필 외에는 자동 비활성. 실 CORS 정책은 ch07-security 에서 다룬다.
 */
@Configuration
@Profile("local")
public class LocalDevCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST");
    }
}
