package com.situnes.repository;

import com.situnes.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
