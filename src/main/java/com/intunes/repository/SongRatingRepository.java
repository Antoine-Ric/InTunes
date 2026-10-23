package com.intunes.repository;

import com.intunes.domain.SongRating;
import com.intunes.domain.MatchupRun;
import com.intunes.domain.Song;
import com.intunes.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SongRatingRepository extends JpaRepository<SongRating, Long> {
    Optional<SongRating> findByRunAndSong(MatchupRun run, Song song);

    List<SongRating> findByRunAndSongInOrderBySongIdAsc(MatchupRun run, Collection<Song> songs);

    List<SongRating> findByRunOrderByRatingDescSongIdAsc(MatchupRun run, Pageable pageable);

    /** User-facing reads use only the latest completed attempt for each song's genre. */
    @Query("select r from SongRating r where r.user = :user and r.song = :song and " + RatingQueries.LATEST_COMPLETED)
    Optional<SongRating> findByUserAndSong(@Param("user") User user, @Param("song") Song song);

    @Query("select r from SongRating r where r.user = :user and r.song in :songs and "
            + RatingQueries.LATEST_COMPLETED + " order by r.song.id")
    List<SongRating> findByUserAndSongIn(@Param("user") User user, @Param("songs") Collection<Song> songs);

    @Query("select r from SongRating r join fetch r.song where r.user = :user and r.genre = :genre and "
            + RatingQueries.LATEST_COMPLETED + " order by r.rating desc, r.song.id")
    List<SongRating> findTopRatedByUserAndGenre(@Param("user") User user, @Param("genre") String genre, Pageable pageable);
}
