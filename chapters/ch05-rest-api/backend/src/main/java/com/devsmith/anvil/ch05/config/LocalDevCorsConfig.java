package com.devsmith.anvil.ch05.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 개발용 CORS — local 프로필 한정. 본격 CORS 는 ch07-security.
 *
 * <p>본 챕터는 5 종류 HTTP 메서드를 모두 쓰므로 PUT/PATCH/DELETE 도 허용.</p>
 */
@Configuration
@Profile("local")
public class LocalDevCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .exposedHeaders("Location");
    }
}
