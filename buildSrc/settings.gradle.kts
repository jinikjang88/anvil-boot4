// buildSrc 는 convention plugin 들이 version catalog 를 참조할 수 있도록
// 루트의 libs.versions.toml 을 명시적으로 import 한다.
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
