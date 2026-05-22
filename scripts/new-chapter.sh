#!/usr/bin/env bash
# 새 챕터 스캐폴딩 도구
# 사용법:
#   ./scripts/new-chapter.sh <id> <slug> "<title>"
# 예시:
#   ./scripts/new-chapter.sh 01 foundation "Spring Boot 4의 세계"
#   ./scripts/new-chapter.sh 09b jooq "jOOQ — SQL-First 비교 세부챕터"
#
# 생성 결과:
#   chapters/ch<id>-<slug>/
#   ├── README.md                            # 8 섹션 템플릿
#   ├── backend/
#   │   ├── build.gradle.kts                 # anvil.spring-boot-conventions 적용
#   │   └── src/{main,test}/java/com/devsmith/anvil/ch<id>/...
#   │   └── src/main/resources/application.yml
#   └── frontend/
#       ├── index.html                       # Tailwind CDN + 오행 팔레트
#       ├── style.css                        # 오행 CSS 변수
#       └── script.js
#
# 주의: settings.gradle.kts 가 chapters/ 를 자동 스캔하므로 별도 등록 불필요.

set -euo pipefail

if [ $# -ne 3 ]; then
  cat <<USAGE
사용법: $0 <id> <slug> "<title>"
예시:   $0 01 foundation "Spring Boot 4의 세계"
        $0 09b jooq "jOOQ — SQL-First 비교 세부챕터"
USAGE
  exit 1
fi

ID="$1"
SLUG="$2"
TITLE="$3"
CHAPTER="ch${ID}-${SLUG}"

# 레포 루트 기준으로 동작 (scripts/ 하위에서 실행되어도 동일하게 동작하도록)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHAPTER_DIR="${ROOT}/chapters/${CHAPTER}"
PKG="com.devsmith.anvil.ch${ID}"
PKG_PATH="com/devsmith/anvil/ch${ID}"
# "01" -> "Ch01Application", "09b" -> "Ch09bApplication"
CLASS_NAME="Ch${ID}Application"

if [ -d "${CHAPTER_DIR}" ]; then
  echo "ERROR: 이미 존재함 → ${CHAPTER_DIR}" >&2
  exit 1
fi

mkdir -p "${CHAPTER_DIR}/backend/src/main/java/${PKG_PATH}"
mkdir -p "${CHAPTER_DIR}/backend/src/main/resources"
mkdir -p "${CHAPTER_DIR}/backend/src/test/java/${PKG_PATH}"
mkdir -p "${CHAPTER_DIR}/frontend"

# ── README.md (8 섹션 템플릿) ────────────────────────────────────────────
cat > "${CHAPTER_DIR}/README.md" <<EOF
# ${CHAPTER} — ${TITLE}

> 챕터 로드맵 및 컨벤션은 \`.claude/CLAUDE.md\` 참조.

## 1. 실생활 비유 (Why)
_왜 이 기술이 필요한가? 일상 예시로._

## 2. 진짜 현장 이야기 (War Story)
_실무에서 이걸 안 썼을 때 무엇이 터졌는가._

## 3. 핵심 개념 (What)
_비유 → 개념 매핑._

## 4. 코드로 벼리기 (How)
\`\`\`bash
# 백엔드 실행
./gradlew :chapters:${CHAPTER}:bootRun

# 프론트엔드 (정적) — 단순히 브라우저로 열거나
python3 -m http.server 5173 --directory chapters/${CHAPTER}/frontend
\`\`\`

## 5. 시각화 (See)
\`frontend/index.html\` — 데모 페이지.

## 6. 실무 체크리스트 (ISMS-P / 성능 / 보안)
- [ ] _체크리스트 항목_

## 7. 흔한 실수 & 디버깅
_내가 겪은 삽질._

## 8. 더 깊이 (선택)
- 공식 문서:
- 관련 글:
EOF

# ── backend/build.gradle.kts ───────────────────────────────────────────
cat > "${CHAPTER_DIR}/backend/build.gradle.kts" <<EOF
// ${CHAPTER} — ${TITLE}
// 공통 컨벤션만 적용. 챕터별 추가 의존성은 아래 dependencies 에 추가.
plugins {
    id("anvil.spring-boot-conventions")
}

description = "${CHAPTER} — ${TITLE}"
EOF

# ── backend/src/main/java/.../Application.java ─────────────────────────
cat > "${CHAPTER_DIR}/backend/src/main/java/${PKG_PATH}/${CLASS_NAME}.java" <<EOF
package ${PKG};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ${CHAPTER} 진입점.
 * 학습 목적 — 챕터별 독립 실행 가능한 최소 Spring Boot 애플리케이션.
 */
@SpringBootApplication
public class ${CLASS_NAME} {
    public static void main(String[] args) {
        SpringApplication.run(${CLASS_NAME}.class, args);
    }
}
EOF

# ── backend/src/main/resources/application.yml ─────────────────────────
cat > "${CHAPTER_DIR}/backend/src/main/resources/application.yml" <<EOF
spring:
  application:
    name: ${CHAPTER}
  profiles:
    active: local

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info
EOF

# ── frontend/index.html (Tailwind CDN + 오행 팔레트) ─────────────────────
cat > "${CHAPTER_DIR}/frontend/index.html" <<EOF
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${CHAPTER} — ${TITLE}</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="stylesheet" href="style.css" />
</head>
<body class="min-h-screen">
  <main class="max-w-4xl mx-auto p-8">
    <header class="border-b border-anvil-muted pb-4 mb-8">
      <p class="text-anvil-muted text-sm">대장간 백엔드 · ${CHAPTER}</p>
      <h1 class="text-3xl font-bold text-anvil-accent mt-1">${TITLE}</h1>
    </header>

    <section id="demo" class="space-y-4">
      <p>이 챕터의 인터랙티브 데모.</p>
    </section>
  </main>
  <script src="script.js"></script>
</body>
</html>
EOF

# ── frontend/style.css (오행 컬러 CSS 변수) ──────────────────────────────
cat > "${CHAPTER_DIR}/frontend/style.css" <<'EOF'
/* 오행 팔레트 — 대장간 + 사주 브랜드 통합 */
:root {
  --anvil-bg:      #F5EFE0; /* 土 — 베이지 배경 */
  --anvil-accent:  #C0392B; /* 火 — 강조/경고 */
  --anvil-data:    #1E5F8C; /* 水 — 데이터/링크 */
  --anvil-success: #3B7A57; /* 木 — 성공/긍정 */
  --anvil-muted:   #7F8C8D; /* 金 — 보조 텍스트 */
  --anvil-fg:      #2C2C2C;
}

body {
  background-color: var(--anvil-bg);
  color: var(--anvil-fg);
  font-family: -apple-system, BlinkMacSystemFont, "Pretendard", "Apple SD Gothic Neo",
               "Segoe UI", Roboto, sans-serif;
}

.text-anvil-accent  { color: var(--anvil-accent); }
.text-anvil-data    { color: var(--anvil-data); }
.text-anvil-success { color: var(--anvil-success); }
.text-anvil-muted   { color: var(--anvil-muted); }
.bg-anvil-accent    { background-color: var(--anvil-accent); }
.bg-anvil-data      { background-color: var(--anvil-data); }
.border-anvil-muted { border-color: var(--anvil-muted); }
EOF

# ── frontend/script.js ────────────────────────────────────────────────
cat > "${CHAPTER_DIR}/frontend/script.js" <<EOF
// ${CHAPTER} 프론트엔드 진입점
// 백엔드 API 와 통신하는 fetch 코드 등을 여기에 작성한다.
console.log('[${CHAPTER}] loaded');
EOF

echo "생성 완료 → chapters/${CHAPTER}/"
echo "  - README.md         (8 섹션 템플릿)"
echo "  - backend/          (Spring Boot 모듈, settings.gradle.kts 가 자동 인식)"
echo "  - frontend/         (Tailwind + 오행 팔레트)"
echo ""
echo "다음:"
echo "  ./gradlew :chapters:${CHAPTER}:bootRun"
