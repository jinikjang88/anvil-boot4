package com.devsmith.anvil.ch08.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 학습 포인트:
 * - record 가 아닌 클래스 — JPA 가 no-arg 생성자 + 가변 컬렉션 + 프록시를 요구하므로 record 불가.
 * - @OneToMany(mappedBy="post", fetch=LAZY) — 기본 LAZY 로 두어 N+1 시연 가능.
 * - toString / equals / hashCode 는 id 만 — 양방향 컬렉션 포함 시 StackOverflowError 함정.
 * - addComment 편의 메서드로 양쪽 동기화 (단방향만 set 하면 영속성 컨텍스트가 헷갈림).
 */
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    protected Post() {
        // JPA 용 기본 생성자 — 직접 호출 금지.
    }

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** 양방향 관계 동기화 — 한 쪽만 set 하면 영속성 컨텍스트가 헷갈린다. */
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
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
        // id 가 null 일 때도 안정적인 hashCode (Hibernate 권장 패턴).
        return Objects.hash(getClass());
    }

    @Override
    public String toString() {
        // comments 절대 포함 금지 — 양방향 순환 → StackOverflowError.
        return "Post{id=" + id + ", title='" + title + "'}";
    }
}
