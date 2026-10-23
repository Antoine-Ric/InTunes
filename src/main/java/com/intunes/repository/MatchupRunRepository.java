package com.intunes.repository;

import com.intunes.domain.MatchupRun;
import com.intunes.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MatchupRunRepository extends JpaRepository<MatchupRun, Long> {
    @Query("select r from MatchupRun r where r.user = :user and r.genre = :genre and r.status = 'IN_PROGRESS'")
    Optional<MatchupRun> findActive(@Param("user") User user, @Param("genre") String genre);

    Optional<MatchupRun> findFirstByUserAndGenreAndStatusOrderByFinishedAtDescIdDesc(
            User user, String genre, MatchupRun.Status status);

    default Optional<MatchupRun> findLatestCompleted(User user, String genre) {
        return findFirstByUserAndGenreAndStatusOrderByFinishedAtDescIdDesc(user, genre, MatchupRun.Status.COMPLETED);
    }

    /** Lock before reading votes or initializing/updating ratings; caller must own the transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from MatchupRun r where r.id = :id and r.user = :user")
    Optional<MatchupRun> findByIdAndUserForUpdate(@Param("id") Long id, @Param("user") User user);
}
