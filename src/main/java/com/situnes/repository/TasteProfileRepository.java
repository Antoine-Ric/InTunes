package com.situnes.repository;

import com.situnes.domain.TasteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TasteProfileRepository extends JpaRepository<TasteProfile, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
