package com.intunes.repository;

import com.intunes.domain.Community;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
