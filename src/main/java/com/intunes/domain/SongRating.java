package com.intunes.domain;

import jakarta.persistence.*;

/**
 * A user's Elo rating for one song within one run.
 * Starts at 1500, moves with every matchup via EloRatingService.
 */
@Entity
@Table(name = "song_ratings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "song_id"}))
public class SongRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MatchupRun run;

    @Column(nullable = false, length = 50)
    private String genre;

    @Version
    private long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Song song;

    @Column(nullable = false)
    private double rating = 1500.0;

    @Column(nullable = false)
    private int matchupsPlayed = 0;

    protected SongRating() {
        // JPA
    }

    public SongRating(MatchupRun run, Song song) {
        this(run, song, 1500.0);
    }

    /** Allows the service to supply intunes.matchup.elo-start-rating. */
    public SongRating(MatchupRun run, Song song, double initialRating) {
        if (!run.getGenre().equals(song.getGenre())) {
            throw new IllegalArgumentException("Song must belong to the run's genre");
        }
        this.run = run;
        this.user = run.getUser();
        this.genre = run.getGenre();
        this.song = song;
        this.rating = initialRating;
    }

    public Long getId() {
        return id;
    }

    public MatchupRun getRun() { return run; }
    public String getGenre() { return genre; }
    public long getVersion() { return version; }

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
