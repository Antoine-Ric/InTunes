package com.intunes.repository;

import com.intunes.domain.IssuedMatchupPair;
import com.intunes.domain.MatchupRun;
import com.intunes.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface IssuedMatchupPairRepository extends JpaRepository<IssuedMatchupPair, UUID> {
    @Query("select p from IssuedMatchupPair p where p.run = :run and p.status = 'ISSUED'")
    Optional<IssuedMatchupPair> findPendingByRun(@Param("run") MatchupRun run);
    Optional<IssuedMatchupPair> findByIdAndRunAndUser(UUID id, MatchupRun run, User user);
    long countByRun(MatchupRun run);
}
