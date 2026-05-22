package com.devsmith.anvil.ch07.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 개발용 CORS — local 프로필 한정.
 * Authorization 헤더를 허용해야 fetch 가 Bearer 토큰을 보낼 수 있다.
 */
@Configuration
@Profile("local")
public class LocalDevCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "Authorization");
    }
}
