package com.devsmith.anvil.ch08.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * 댓글 엔티티.
 *
 * 학습 포인트:
 * - @ManyToOne 기본은 EAGER 인데 일부러 LAZY 로 강제 — N+1 의 핵심 함정 (역방향).
 * - @JoinColumn(name="post_id") + 인덱스 — FK 컬럼은 자주 인덱스 누락이 슬로우 쿼리 원인.
 */
@Entity
@Table(name = "comments", indexes = @Index(name = "idx_comments_post_id", columnList = "post_id"))
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false, length = 50)
    private String author;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Comment() {
        // JPA 용 기본 생성자.
    }

    public Comment(String author, String body) {
        this.author = author;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public String getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }

    /** 패키지-프라이빗 setter — 외부에서는 Post.addComment() 로만 연결. */
    void setPost(Post post) {
        this.post = post;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        // post 절대 포함 금지 — 양방향 순환 함정.
        return "Comment{id=" + id + ", author='" + author + "'}";
    }
}
