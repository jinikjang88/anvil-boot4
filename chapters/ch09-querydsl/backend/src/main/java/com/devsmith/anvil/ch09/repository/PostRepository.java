package com.devsmith.anvil.ch09.repository;

import com.devsmith.anvil.ch09.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 기본 인터페이스 + 커스텀 인터페이스 합성.
 *
 * 학습 포인트: Spring Data 의 "메서드 이름 파생" 으로는 동적 조건 5개를 표현 불가.
 *   findByKeywordAndAuthorAndCreatedAtBetweenAndCommentsIsNotEmpty(...) 같은 메서드는
 *   "조건이 null 이면 빼고" 가 안 된다 (메서드 시그니처 자체가 결정).
 *
 * → 커스텀 인터페이스 (PostQueryRepository) 를 같이 상속해 QueryDSL 구현체에 위임.
 *   Spring Data 규약: 같은 이름의 Impl 클래스 (PostQueryRepositoryImpl) 를 자동 결합.
 */
public interface PostRepository extends JpaRepository<Post, Long>, PostQueryRepository {
}
