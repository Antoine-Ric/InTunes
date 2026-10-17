package com.situnes.repository;

import com.situnes.domain.Community;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
