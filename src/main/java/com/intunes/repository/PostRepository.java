package com.intunes.repository;

import com.intunes.domain.Post;
import com.intunes.domain.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCommunityOrderByCreatedAtDescIdDesc(Community community, Pageable pageable);
}
