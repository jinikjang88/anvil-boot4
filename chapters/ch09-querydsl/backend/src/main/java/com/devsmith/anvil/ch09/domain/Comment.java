package com.devsmith.anvil.ch09.domain;

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
 * - "댓글 있는 글만" 조건을 QueryDSL exists 서브쿼리로 표현하기 위해 존재.
 * - @ManyToOne LAZY — N+1 함정 자체는 ch08 에서 시연, ch09 는 fetchJoin 활용에 집중.
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
        return "Comment{id=" + id + ", author='" + author + "'}";
    }
}
