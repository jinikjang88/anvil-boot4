// Java 공통 컨벤션
// - Java 25 toolchain (Foojay resolver 가 자동 다운로드)
// - UTF-8 인코딩 고정
// - JUnit 5 + AssertJ 테스트 환경
//
// 적용: 모든 챕터 build.gradle.kts 에서 `id("anvil.java-conventions")` 만 선언하면 됨.

plugins {
    `java-library`
}

// version catalog 접근을 위한 헬퍼 (convention plugin 에서는 type-safe accessor 미지원)
val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // 학습 목적 — 컴파일러 경고를 끄지 말고 노출한다.
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}

dependencies {
    // 모든 모듈 공통 테스트 의존성 — given/when/then 작성에 사용
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
}
