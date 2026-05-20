// 대장간 백엔드 — 루트 설정
// - 모든 챕터를 동적으로 include (chapters/chXX-* 패턴)
// - Foojay Toolchain Resolver: Java 25 toolchain 자동 프로비저닝
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "anvil-boot4"

// chapters/chXX-*/backend/build.gradle.kts 를 가진 모듈만 Gradle 에 포함시킨다.
// frontend/ 는 정적 자산이므로 빌드 대상이 아니다.
// 모듈 경로는 `:chapters:chXX-<topic>` 으로 평탄화하여 챕터 간 의존 금지 원칙을 유지한다.
file("chapters").listFiles()
    ?.filter { it.isDirectory && File(it, "backend/build.gradle.kts").exists() }
    ?.forEach { dir ->
        val name = dir.name
        include(":chapters:$name")
        project(":chapters:$name").projectDir = File(dir, "backend")
    }
