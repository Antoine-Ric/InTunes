package com.intunes.domain;

import jakarta.persistence.*;

/**
 * A user's Elo rating for one song within one genre.
 * Starts at 1500, moves with every matchup via EloRatingService.
 * TODO (ADR-0003): scope this entity and its uniqueness to a run before
 * implementing matchup persistence; the current mapping is the scaffold.
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

    protected SongRating() {
        // JPA
    }

    public SongRating(User user, Song song) {
        this(user, song, 1500.0);
    }

    /** Allows the service to supply intunes.matchup.elo-start-rating. */
    public SongRating(User user, Song song, double initialRating) {
        this.user = user;
        this.song = song;
        this.rating = initialRating;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Song getSong() {
        return song;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getMatchupsPlayed() {
        return matchupsPlayed;
    }

    public void setMatchupsPlayed(int matchupsPlayed) {
        this.matchupsPlayed = matchupsPlayed;
    }
}
