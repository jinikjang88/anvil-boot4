package com.devsmith.anvil.ch09.service;

import com.devsmith.anvil.ch09.controller.PostDtos.CommentSummary;
import com.devsmith.anvil.ch09.controller.PostDtos.PostResponse;
import com.devsmith.anvil.ch09.domain.Comment;
import com.devsmith.anvil.ch09.domain.Post;
import com.devsmith.anvil.ch09.repository.PostRepository;
import com.devsmith.anvil.ch09.search.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 검색 도메인 서비스. 세 가지 구현을 트랜잭션 안에서 호출하고 DTO 로 매핑한다.
 *
 * OSIV=false 이므로 LAZY 관계(comments) 접근은 반드시 이 서비스의 트랜잭션 안에서 끝낸다.
 */
@Service
public class PostSearchService {

    private final PostRepository repository;

    public PostSearchService(PostRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PostResponse> searchByJpql(PostSearchCondition cond) {
        return repository.searchByJpqlConcat(cond).stream()
                .map(PostSearchService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> searchByBuilder(PostSearchCondition cond, Pageable pageable) {
        Page<Post> page = repository.searchByBooleanBuilder(cond, pageable);
        return mapPage(page, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> searchByExpression(PostSearchCondition cond, Pageable pageable) {
        Page<Post> page = repository.searchByBooleanExpression(cond, pageable);
        return mapPage(page, pageable);
    }

    private static Page<PostResponse> mapPage(Page<Post> page, Pageable pageable) {
        List<PostResponse> mapped = page.getContent().stream()
                .map(PostSearchService::toResponse)
                .toList();
        return new PageImpl<>(mapped, pageable, page.getTotalElements());
    }

    private static PostResponse toResponse(Post post) {
        List<CommentSummary> comments = post.getComments().stream()
                .map(c -> new CommentSummary(c.getId(), c.getAuthor()))
                .toList();
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getCreatedAt(),
                comments.size(),
                comments
        );
    }

    @Transactional
    public PostResponse create(String title, String content, String author) {
        return toResponse(repository.save(new Post(title, content, author)));
    }

    @Transactional
    public PostResponse addComment(Long postId, String author, String body) {
        Post post = repository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
        post.addComment(new Comment(author, body));
        return toResponse(post);
    }
}
