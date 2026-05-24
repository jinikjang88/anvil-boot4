// ch09-querydsl — QueryDSL과 동적 쿼리
//
// 핵심 빌드 설정: QueryDSL 의 Q클래스 자동 생성.
// - querydsl-jpa:5.1.0:jakarta  → 런타임 (JPAQueryFactory, BooleanBuilder 등)
// - querydsl-apt:5.1.0:jakarta  → 컴파일 시 @Entity 를 보고 QPost / QComment 생성
// - jakarta.persistence-api / jakarta.annotation-api → APT 가 어노테이션 메타데이터를 읽으려면 필요
//
// 함정: classifier "jakarta" 를 빼면 javax.persistence.* 를 import 한 Q클래스가 생성되어 컴파일 실패.
//       Boot 4 / Hibernate 6 부터는 무조건 jakarta classifier.
plugins {
    id("anvil.spring-boot-conventions")
}

description = "ch09-querydsl — QueryDSL과 동적 쿼리"

// QueryDSL 의존성 / 자동 생성 Q클래스가 만드는 lint 경고 억제.
//   - classfile : querydsl-core 가 IntelliJ 의 org.jetbrains.annotations.Range 를 optional 참조
//   - this-escape : Q클래스 생성자가 createString(...) 같은 부모 메서드를 호출 (안전한 패턴)
// 학습 출력이 경고로 가려지는 걸 막기 위해 이 챕터만 명시적으로 끈다.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:-classfile", "-Xlint:-this-escape"))
}

dependencies {
    // JPA — 도메인 매핑은 ch08 과 같지만 챕터 간 코드 의존 금지 원칙으로 재정의.
    implementation(libs.spring.boot.starter.data.jpa)

    // QueryDSL 런타임 — JPAQueryFactory, BooleanBuilder, BooleanExpression
    implementation("${libs.querydsl.jpa.get().module}:${libs.versions.querydsl.get()}:jakarta")

    // QueryDSL 어노테이션 프로세서 — Q클래스 생성
    annotationProcessor("${libs.querydsl.apt.get().module}:${libs.versions.querydsl.get()}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // PostgreSQL JDBC 드라이버
    runtimeOnly(libs.postgresql.driver)

    // 테스트 — Testcontainers PostgreSQL (실제 DB 방언 확인)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}
