package com.devsmith.anvil.ch09b.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 개발용 CORS — local 프로필 한정.
 * frontend/ 를 python http.server 로 띄울 때 (포트 5173~5175) fetch 가 막히지 않도록.
 */
@Configuration
@Profile("local")
public class LocalDevCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173", "http://127.0.0.1:5173",
                        "http://localhost:5174", "http://127.0.0.1:5174",
                        "http://localhost:5175", "http://127.0.0.1:5175"
                )
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type");
    }
}
