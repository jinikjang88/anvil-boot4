package com.devsmith.anvil.ch09.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 게시글 엔티티.
 *
 * ch08 의 Post 와 같은 모양 — 챕터 간 코드 의존을 금지하는 규칙에 따라 재정의.
 *
 * 학습 포인트 (ch09 관점):
 * - QueryDSL APT 가 컴파일 시 이 클래스를 보고 QPost (build/generated/.../QPost.java) 를 만든다.
 * - 검색 대상 컬럼은 인덱스를 같이 — 학습용 작은 데이터에서도 운영 감각을 익히려는 습관.
 * - author 컬럼 추가: ch08 (제목+본문) 보다 검색 조건을 늘려 동적 쿼리의 가치를 보여줌.
 */
@Entity
@Table(
    name = "posts",
    indexes = {
        @Index(name = "idx_posts_author",     columnList = "author"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
    }
)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** 작성자 — 검색 동적 조건 데모용. */
    @Column(nullable = false, length = 50)
    private String author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    protected Post() {
        // JPA 용 기본 생성자 — 직접 호출 금지.
    }

    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    /** 시드 데이터 재현성을 위해 createdAt 을 임의 시각으로 주입 가능. */
    public Post(String title, String content, String author, Instant createdAt) {
        this(title, content, author);
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Comment> getComments() { return comments; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        return "Post{id=" + id + ", title='" + title + "', author='" + author + "'}";
    }
}
