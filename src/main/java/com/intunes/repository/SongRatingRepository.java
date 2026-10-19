package com.intunes.repository;

import com.intunes.domain.SongRating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRatingRepository extends JpaRepository<SongRating, Long> {
    // TODO: query methods as needed, e.g. findByUsername
}
