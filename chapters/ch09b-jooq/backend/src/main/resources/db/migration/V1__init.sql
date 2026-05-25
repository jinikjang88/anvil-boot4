-- ch09b-jooq Flyway 마이그레이션 (참고용)
--
-- 현재 학습 모드(Lite)에서는 SchemaInitializer.java 가 부팅마다 DROP+CREATE.
-- Full 모드(codegen + Flyway)에서는 이 파일이 스키마의 "진실의 원천(Single Source of Truth)".
--
-- Flyway 런타임 자동 적용을 켜려면:
--   1. build.gradle.kts 에 implementation("org.flywaydb:flyway-database-postgresql") 추가
--   2. application.yml 에 spring.flyway.enabled=true
--   3. SchemaInitializer 의 @Order(HIGHEST_PRECEDENCE) 제거 (Flyway 가 먼저 실행되므로 불필요)
--
-- 아래 DDL 은 SchemaInitializer.java 와 동일 — 학습자가 diff 로 확인 가능.

CREATE TABLE posts (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    author      VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_posts_title_author UNIQUE (title, author)
);

CREATE INDEX idx_posts_author     ON posts(author);
CREATE INDEX idx_posts_created_at ON posts(created_at);

CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    parent_id   BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    author      VARCHAR(50) NOT NULL,
    body        VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post_id   ON comments(post_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);
