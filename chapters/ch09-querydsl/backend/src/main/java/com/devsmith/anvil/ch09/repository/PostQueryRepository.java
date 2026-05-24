package com.devsmith.anvil.ch09.repository;

import com.devsmith.anvil.ch09.domain.Post;
import com.devsmith.anvil.ch09.search.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 커스텀 리포지토리 인터페이스 — QueryDSL 구현부와 JPA 표준 인터페이스를 분리하는 표준 패턴.
 *
 * - JPQL 문자열 버전과 QueryDSL 두 스타일 (BooleanBuilder / BooleanExpression) 을
 *   같은 인터페이스로 노출해 컨트롤러가 한 줄로 호출하게 한다.
 *
 * 학습 포인트: 인터페이스/구현 분리 덕에 컨트롤러는 "어떻게 쿼리하는지" 를 모른다.
 */
public interface PostQueryRepository {

    /** 안티패턴 시연 — JPQL String concat. SQL Injection 위험은 없지만 깨지기 쉽고 가독성 낮음. */
    List<Post> searchByJpqlConcat(PostSearchCondition cond);

    /** QueryDSL BooleanBuilder 스타일 — 명령형. if 문으로 조건 추가. */
    Page<Post> searchByBooleanBuilder(PostSearchCondition cond, Pageable pageable);

    /** QueryDSL BooleanExpression 스타일 — 함수형. null 반환 시 자동 무시. (권장) */
    Page<Post> searchByBooleanExpression(PostSearchCondition cond, Pageable pageable);
}
