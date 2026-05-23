package com.devsmith.anvil.ch08.repository;

import com.devsmith.anvil.ch08.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
