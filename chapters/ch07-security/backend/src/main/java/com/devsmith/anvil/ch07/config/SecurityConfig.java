package com.devsmith.anvil.ch07.config;

import com.devsmith.anvil.ch07.security.JwtAccessDeniedHandler;
import com.devsmith.anvil.ch07.security.JwtAuthenticationEntryPoint;
import com.devsmith.anvil.ch07.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 필터 체인 + @PreAuthorize 활성화.
 *
 * <p>핵심 설정:
 * <ul>
 *   <li>CSRF 비활성 — JWT 기반 stateless API 이므로 (브라우저 폼/쿠키 시나리오 아님)</li>
 *   <li>세션 STATELESS — 서버에 세션 저장 안 함, 매 요청 JWT 로 인증</li>
 *   <li>{@code /api/v1/auth/**} 는 공개 (회원가입/로그인/디코더)</li>
 *   <li>{@code /api/v1/users/**} 는 인증 필요</li>
 *   <li>{@code @PreAuthorize} 로 추가 권한 체크 ({@code hasRole('ADMIN')} 등)</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 기본 strength=10. salt 자동 생성, 같은 평문도 매번 다른 해시.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           JwtAuthenticationEntryPoint entryPoint,
                                           JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())            // LocalDevCorsConfig 가 등록한 CorsConfigurationSource 사용
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
