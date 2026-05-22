// buildSrc — convention plugin 컴파일을 위해 kotlin-dsl 만 적용한다.
// 여기에 적용한 플러그인은 모든 모듈 build.gradle.kts 에서 id("anvil.*") 로 참조 가능.
plugins {
    `kotlin-dsl`
}

dependencies {
    // convention plugin 안에서 외부 Gradle plugin 을 적용하려면
    // 해당 plugin 의 marker artifact 를 implementation 으로 추가해야 한다.
    // (Gradle plugin DSL 한계 우회 — 공식 권장 패턴)
    implementation(libs.plugins.spring.boot.toCoordinates())
    implementation(libs.plugins.spring.dependency.management.toCoordinates())

    // precompiled script plugin 안에서 `the<LibrariesForLibs>()` 로 version catalog
    // 에 접근하려면, 카탈로그가 생성한 클래스(jar)를 buildSrc 컴파일 classpath 에 노출해야 한다.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

// Plugin marker 좌표 변환 헬퍼:
//   id = "org.springframework.boot", version = "4.0.0"
//   → "org.springframework.boot:org.springframework.boot.gradle.plugin:4.0.0"
fun Provider<PluginDependency>.toCoordinates(): Provider<String> = map { plugin ->
    "${plugin.pluginId}:${plugin.pluginId}.gradle.plugin:${plugin.version}"
}
