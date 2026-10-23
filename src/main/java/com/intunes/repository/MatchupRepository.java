package com.intunes.repository;

import com.intunes.domain.Matchup;
import com.intunes.domain.MatchupRun;
import com.intunes.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MatchupRepository extends JpaRepository<Matchup, Long> {
    /** Historical votes across attempts. Never use this count for run completion. */
    @Query("select m from Matchup m where m.user = :user and m.genre = :genre order by m.createdAt, m.id")
    List<Matchup> findByUserAndGenre(@Param("user") User user, @Param("genre") String genre);

    long countByUserAndGenre(User user, String genre);

    List<Matchup> findByRunOrderByCreatedAtAscIdAsc(MatchupRun run);

    long countByRun(MatchupRun run);
}
