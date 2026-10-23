package com.intunes.repository;

import com.intunes.domain.Matchup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchupRepository extends JpaRepository<Matchup, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
