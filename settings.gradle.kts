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

// chapters/ 하위 디렉터리 중 build.gradle.kts 가 있는 모듈을 자동 include
// 챕터 간 의존 금지 원칙을 지키기 위해 평탄한 include 구조를 사용한다.
file("chapters").listFiles()
    ?.filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    ?.forEach { dir ->
        val name = dir.name
        include(":chapters:$name")
        project(":chapters:$name").projectDir = dir
    }
