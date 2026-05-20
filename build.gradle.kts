// 루트 빌드 — 직접 빌드 대상 코드 없음.
// 모든 챕터/모듈 공통 설정은 buildSrc 의 convention plugin 으로 분리한다.
//   - anvil.java-conventions: Java 25 toolchain + 테스트 공통
//   - anvil.spring-boot-conventions: 위 + Spring Boot 4 + dependency management
//
// 루트에서는 편의 task 만 정의한다.

tasks.register("printModules") {
    group = "help"
    description = "포함된 모든 서브모듈 이름을 출력한다."
    doLast {
        subprojects.forEach { println(" - ${it.path}") }
    }
}
