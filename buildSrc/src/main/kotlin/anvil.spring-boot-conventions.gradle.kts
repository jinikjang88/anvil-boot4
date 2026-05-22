// Spring Boot 공통 컨벤션
// - anvil.java-conventions 위에 Spring Boot 4 와 dependency-management 를 얹는다.
// - 각 챕터는 이 convention 하나만 적용하면 실행 가능한 Spring Boot 앱이 된다.
//
// 적용: chapters/chXX-* 의 build.gradle.kts 에서 `id("anvil.spring-boot-conventions")`.

plugins {
    id("anvil.java-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencyManagement {
    imports {
        // Spring Boot BOM 으로 모든 starter 의 버전을 일괄 관리
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
        mavenBom(libs.testcontainers.bom.get().toString())
    }
}

dependencies {
    // 학습 모듈 기본 starter — 챕터별로 추가 starter 만 더하면 됨
    "implementation"(libs.spring.boot.starter.web)
    "implementation"(libs.spring.boot.starter.actuator)
    "implementation"(libs.spring.boot.starter.validation)

    // 테스트는 Spring Boot Test 스타터로 전환 (anvil.java-conventions 의 JUnit 위에 덧붙음)
    "testImplementation"(libs.spring.boot.starter.test)
    // Boot 4 에서 WebMvcTest / @AutoConfigureMockMvc 슬라이스 테스트는 별도 모듈
    "testImplementation"(libs.spring.boot.webmvc.test)
}
