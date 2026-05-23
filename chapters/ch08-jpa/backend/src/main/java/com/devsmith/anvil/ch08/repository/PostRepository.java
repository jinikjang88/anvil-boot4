package com.devsmith.anvil.ch08.repository;

import com.devsmith.anvil.ch08.domain.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 두 가지 조회 메서드를 의도적으로 나란히 둔다.
 *
 * - findAll() : Spring Data 기본. comments 는 LAZY 프록시 → 접근 시 N+1.
 * - findAllWithComments() : @EntityGraph 로 comments 까지 한 번에 fetch.
 *
 * 학습 의도: 같은 도메인을 두 호출로 비교해 N+1 의 정체를 눈으로 확인.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * JPQL 의 distinct 가 카테시안 곱으로 부풀려진 부모 행을 중복 제거한다.
     * @EntityGraph 가 comments 를 join fetch 로 같이 가져오게 만든다.
     */
    @EntityGraph(attributePaths = "comments")
    @Query("select distinct p from Post p order by p.id")
    List<Post> findAllWithComments();
}
