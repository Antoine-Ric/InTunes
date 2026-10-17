package com.situnes.repository;

import com.situnes.domain.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
