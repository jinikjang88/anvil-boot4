// ch07-security — 보안 기초 — JWT + BCrypt + AES + 인가
plugins {
    id("anvil.spring-boot-conventions")
}

description = "ch07-security — 보안 기초 — JWT + BCrypt + AES + 인가"

dependencies {
    // Spring Security 본체 — BCryptPasswordEncoder, SecurityFilterChain, @PreAuthorize 등
    implementation(libs.spring.boot.starter.security)

    // Boot 4: starter-web 만으로 jackson-databind 가 컴파일 클래스패스에 안 잡혀 ObjectMapper import 가 실패.
    // EntryPoint / AccessDeniedHandler 에서 ProblemDetail 을 직접 직렬화하므로 명시 의존.
    implementation(libs.spring.boot.starter.json)

    // JWT — io.jsonwebtoken (jjwt 0.12)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // 테스트 — Security 컨텍스트 모킹 (@WithMockUser 등)
    testImplementation(libs.spring.security.test)
}
