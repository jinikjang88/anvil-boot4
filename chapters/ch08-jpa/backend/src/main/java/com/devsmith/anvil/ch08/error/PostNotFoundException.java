package com.devsmith.anvil.ch08.error;

/**
 * 존재하지 않는 게시글 id 로 접근했을 때.
 * RestExceptionHandler 가 404 ProblemDetail 로 변환.
 */
public class PostNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PostNotFoundException(Long id) {
        super("Post not found: id=" + id);
    }
}
