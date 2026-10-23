package com.intunes.repository;

import com.intunes.domain.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findBySpotifyId(String spotifyId);

    /** Seeding dedupe: same song, same genre bucket, regardless of casing. */
    boolean existsByTitleIgnoreCaseAndArtistIgnoreCaseAndGenre(String title, String artist, String genre);

    List<Song> findByGenre(String genre);
}
