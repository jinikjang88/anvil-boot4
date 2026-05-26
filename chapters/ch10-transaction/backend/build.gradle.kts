// ch10-transaction — 트랜잭션 — 격리·전파·AOP 함정
//
// 도메인: 은행 계좌(Account) 송금. 잔액 합계 불변(conservation) 으로 트랜잭션 정합성을 즉각 검증.
// JPA + @Transactional 이 핵심 — 격리/전파/AOP 세 축을 의도적으로 깨고 고치며 학습.
plugins {
    id("anvil.spring-boot-conventions")
}

description = "ch10-transaction — 트랜잭션 — 격리·전파·AOP 함정"

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    runtimeOnly(libs.postgresql.driver)

    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}
