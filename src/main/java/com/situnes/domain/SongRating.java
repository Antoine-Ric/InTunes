package com.situnes.domain;

import jakarta.persistence.*;

/**
 * A user's Elo rating for one song within one genre.
 * Starts at 1500, moves with every matchup via EloRatingService.
 */
@Entity
@Table(name = "song_ratings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "song_id"}))
public class SongRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Song song;

    @Column(nullable = false)
    private double rating = 1500.0;

    @Column(nullable = false)
    private int matchupsPlayed = 0;

    // TODO: getters/setters
}
