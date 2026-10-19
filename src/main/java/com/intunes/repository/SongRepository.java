package com.intunes.repository;

import com.intunes.domain.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
