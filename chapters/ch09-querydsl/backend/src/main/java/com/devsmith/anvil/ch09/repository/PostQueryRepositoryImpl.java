package com.devsmith.anvil.ch09.repository;

import com.devsmith.anvil.ch09.domain.Post;
import com.devsmith.anvil.ch09.domain.QComment;
import com.devsmith.anvil.ch09.domain.QPost;
import com.devsmith.anvil.ch09.search.PostSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * QueryDSL 구현체 — 세 가지 스타일을 한 클래스에 모아 비교 학습용.
 *
 * Spring Data 규약: 같은 패키지에 "PostQueryRepository" + "PostQueryRepositoryImpl" 네이밍 →
 * 자동으로 PostRepository 의 일부로 합쳐진다.
 *
 * 핵심 비교:
 * - searchByJpqlConcat        : 안티패턴. if 로 WHERE 절을 문자열 누적. 컴파일러가 검증 못 함.
 * - searchByBooleanBuilder    : 명령형. if 문 → builder.and(...). QueryDSL 첫 단계 학습.
 * - searchByBooleanExpression : 함수형. private 메서드가 BooleanExpression 또는 null 반환.
 *                                where(...) 가 null 을 자동 무시. (권장 스타일)
 *
 * ch09b-jOOQ 에서 다룰 영역 (QueryDSL 의 약점):
 * - PostgreSQL 윈도우 함수 (ROW_NUMBER OVER PARTITION BY ...) — QueryDSL 미지원
 * - CTE / WITH RECURSIVE — QueryDSL 미지원
 * - DB-side 함수 (jsonb_path_query, FOR UPDATE SKIP LOCKED) — 우회 필요
 * - 벌크 UPSERT (ON CONFLICT ... DO UPDATE) — JPA 패러다임에 안 맞음
 */
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public PostQueryRepositoryImpl(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        this.queryFactory = queryFactory;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1) JPQL String concat — 안티패턴 시연
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * 깨지기 쉬운 코드의 표본.
     *   - 컬럼명/엔티티명 오타가 런타임에야 발견됨.
     *   - 정렬/페이징을 추가하려면 다시 if 문 더미.
     *   - 파라미터 바인딩은 setParameter 로 분리해야 SQL Injection 회피 (실수하면 즉시 취약점).
     *
     * 학습 의도: "왜 QueryDSL 이 필요한가" 를 비교군으로 보여주기 위해 일부러 작성.
     */
    @Override
    public List<Post> searchByJpqlConcat(PostSearchCondition cond) {
        StringBuilder jpql = new StringBuilder("select distinct p from Post p where 1=1");
        List<Object[]> params = new ArrayList<>();

        if (StringUtils.hasText(cond.keyword())) {
            jpql.append(" and (lower(p.title) like :kw or lower(p.content) like :kw)");
            params.add(new Object[]{"kw", "%" + cond.keyword().toLowerCase() + "%"});
        }
        if (StringUtils.hasText(cond.author())) {
            jpql.append(" and p.author = :author");
            params.add(new Object[]{"author", cond.author()});
        }
        if (cond.from() != null) {
            jpql.append(" and p.createdAt >= :from");
            params.add(new Object[]{"from", cond.from()});
        }
        if (cond.to() != null) {
            jpql.append(" and p.createdAt < :to");
            params.add(new Object[]{"to", cond.to()});
        }
        if (cond.hasComments() != null) {
            // exists 서브쿼리 — 메서드 이름으로는 표현 불가, 문자열로는 가독성 끔찍.
            if (cond.hasComments()) {
                jpql.append(" and exists (select 1 from Comment c where c.post = p)");
            } else {
                jpql.append(" and not exists (select 1 from Comment c where c.post = p)");
            }
        }
        jpql.append(" order by p.createdAt desc");

        var query = em.createQuery(jpql.toString(), Post.class);
        for (Object[] p : params) {
            query.setParameter((String) p[0], p[1]);
        }
        return query.getResultList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2) QueryDSL BooleanBuilder — 명령형
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * BooleanBuilder 는 변경 가능한 조건 컨테이너.
     *   - if 로 한 줄씩 추가 — JPQL concat 과 구조가 비슷하지만,
     *     컬럼 참조가 QPost.post.title 처럼 타입 세이프.
     *   - 페이징/정렬은 Querydsl 헬퍼로 깔끔히.
     */
    @Override
    public Page<Post> searchByBooleanBuilder(PostSearchCondition cond, Pageable pageable) {
        QPost post = QPost.post;

        BooleanBuilder where = new BooleanBuilder();
        if (StringUtils.hasText(cond.keyword())) {
            String kw = "%" + cond.keyword().toLowerCase() + "%";
            where.and(post.title.lower().like(kw).or(post.content.lower().like(kw)));
        }
        if (StringUtils.hasText(cond.author())) {
            where.and(post.author.eq(cond.author()));
        }
        if (cond.from() != null) {
            where.and(post.createdAt.goe(cond.from()));
        }
        if (cond.to() != null) {
            where.and(post.createdAt.lt(cond.to()));
        }
        if (cond.hasComments() != null) {
            BooleanExpression hasAny = hasAnyComment();
            where.and(cond.hasComments() ? hasAny : hasAny.not());
        }

        JPAQuery<Post> query = queryFactory.selectFrom(post).where(where);

        long total = queryFactory.select(post.count()).from(post).where(where).fetchOne();
        List<Post> content = applyPaging(query, pageable);

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3) QueryDSL BooleanExpression — 함수형 (권장)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * private 메서드가 BooleanExpression 또는 null 을 반환 →
     * where(...) 의 가변 인자 중 null 은 자동으로 무시된다.
     *
     * 장점:
     *  - 조건 추가/삭제가 매우 짧음 (한 줄)
     *  - 각 조건이 메서드 이름으로 문서화됨 (titleOrContentContains, authorEq, ...)
     *  - 다른 쿼리에서 재사용 가능 (예: count 쿼리와 동일 조건)
     *
     * 함정:
     *  - null 무시는 편하지만 "조건이 빠진 줄 모르고 전체 조회되는" 사고 가능.
     *    → 통합 테스트로 "조건 미입력 시 결과 N건" 같은 회귀 테스트 필수.
     */
    @Override
    public Page<Post> searchByBooleanExpression(PostSearchCondition cond, Pageable pageable) {
        QPost post = QPost.post;

        JPAQuery<Post> query = queryFactory
                .selectFrom(post)
                .where(
                        titleOrContentContains(cond.keyword()),
                        authorEq(cond.author()),
                        createdAtGoe(cond.from()),
                        createdAtLt(cond.to()),
                        commentsExist(cond.hasComments())
                );

        // count 쿼리는 동일 조건을 재사용 — DRY.
        JPAQuery<Long> countQuery = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        titleOrContentContains(cond.keyword()),
                        authorEq(cond.author()),
                        createdAtGoe(cond.from()),
                        createdAtLt(cond.to()),
                        commentsExist(cond.hasComments())
                );

        List<Post> content = applyPaging(query, pageable);

        // PageableExecutionUtils — content < pageSize 면 count 쿼리 생략 (마지막 페이지 최적화).
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 조건 빌더 (함수형 스타일의 핵심)
    // ─────────────────────────────────────────────────────────────────────────────

    private BooleanExpression titleOrContentContains(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        String kw = "%" + keyword.toLowerCase() + "%";
        QPost post = QPost.post;
        return post.title.lower().like(kw).or(post.content.lower().like(kw));
    }

    private BooleanExpression authorEq(String author) {
        return StringUtils.hasText(author) ? QPost.post.author.eq(author) : null;
    }

    private BooleanExpression createdAtGoe(Instant from) {
        return from != null ? QPost.post.createdAt.goe(from) : null;
    }

    private BooleanExpression createdAtLt(Instant to) {
        return to != null ? QPost.post.createdAt.lt(to) : null;
    }

    private BooleanExpression commentsExist(Boolean hasComments) {
        if (hasComments == null) return null;
        BooleanExpression any = hasAnyComment();
        return hasComments ? any : any.not();
    }

    /** 댓글 존재 여부 — QueryDSL 의 JPAExpressions 로 서브쿼리. */
    private BooleanExpression hasAnyComment() {
        QComment c = QComment.comment;
        QPost post = QPost.post;
        return JPAExpressions.selectOne().from(c).where(c.post.eq(post)).exists();
    }

    /**
     * 페이징/정렬을 직접 적용. Spring Data 의 Querydsl 헬퍼를 쓸 수도 있지만
     * 학습용으로는 명시적인 코드가 더 명확하다.
     *
     * 정렬: 사용자가 Sort 를 안 줬으면 createdAt desc 기본값.
     *      직접 컬럼명을 화이트리스트로 매핑 — 외부 입력 그대로 컬럼 참조 금지.
     */
    private List<Post> applyPaging(JPAQuery<Post> query, Pageable pageable) {
        QPost post = QPost.post;

        if (pageable.getSort().isUnsorted()) {
            query.orderBy(post.createdAt.desc());
        } else {
            for (Sort.Order order : pageable.getSort()) {
                ComparableExpressionBase<?> path = switch (order.getProperty()) {
                    case "createdAt" -> post.createdAt;
                    case "author"    -> post.author;
                    case "id"        -> post.id;
                    default -> throw new IllegalArgumentException("정렬 불가 컬럼: " + order.getProperty());
                };
                query.orderBy(new OrderSpecifier<>(order.isAscending() ? Order.ASC : Order.DESC, path));
            }
        }
        return query.offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
