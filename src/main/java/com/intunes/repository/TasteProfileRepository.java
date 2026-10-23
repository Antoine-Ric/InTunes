package com.intunes.repository;

import com.intunes.domain.TasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TasteProfileRepository extends JpaRepository<TasteProfile, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
