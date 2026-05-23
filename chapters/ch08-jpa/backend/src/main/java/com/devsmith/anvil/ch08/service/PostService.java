package com.devsmith.anvil.ch08.service;

import com.devsmith.anvil.ch08.controller.PostDtos.CommentResponse;
import com.devsmith.anvil.ch08.controller.PostDtos.PostResponse;
import com.devsmith.anvil.ch08.domain.Comment;
import com.devsmith.anvil.ch08.domain.Post;
import com.devsmith.anvil.ch08.error.PostNotFoundException;
import com.devsmith.anvil.ch08.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 / 댓글 도메인 서비스.
 *
 * 두 가지 목록 조회를 의도적으로 분리:
 * - listLazy()  : findAll() + LAZY 접근 → N+1 발생 (게시글 1 + 댓글 N 쿼리)
 * - listFetch() : @EntityGraph 로 한 번에 fetch → 1~2 쿼리로 끝
 *
 * 트랜잭션 안에서 LAZY 접근까지 끝낸 뒤 DTO 로 반환.
 * (OSIV=false 라 컨트롤러로 LazyInitializationException 새는 것 방지.)
 */
@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * N+1 시연용 — comments 를 LAZY 로 두고 DTO 매핑 단계에서 접근하면
     * 게시글 수만큼 추가 select 가 나간다.
     */
    @Transactional(readOnly = true)
    public List<PostResponse> listLazy() {
        List<Post> posts = postRepository.findAll();
        return posts.stream().map(PostService::toResponse).toList();
    }

    /**
     * 해법 1 — @EntityGraph 로 comments 까지 한 번에 가져옴.
     */
    @Transactional(readOnly = true)
    public List<PostResponse> listFetch() {
        List<Post> posts = postRepository.findAllWithComments();
        return posts.stream().map(PostService::toResponse).toList();
    }

    @Transactional
    public PostResponse create(String title, String content) {
        Post saved = postRepository.save(new Post(title, content));
        return toResponse(saved);
    }

    @Transactional
    public PostResponse addComment(Long postId, String author, String body) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        post.addComment(new Comment(author, body));
        // cascade=ALL 로 Post.flush 때 Comment 도 함께 INSERT.
        return toResponse(post);
    }

    private static PostResponse toResponse(Post post) {
        List<CommentResponse> comments = post.getComments().stream()
                .map(c -> new CommentResponse(c.getId(), c.getAuthor(), c.getBody(), c.getCreatedAt()))
                .toList();
        return new PostResponse(post.getId(), post.getTitle(), post.getContent(),
                post.getCreatedAt(), comments);
    }
}
