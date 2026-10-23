package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One head-to-head vote: the user picked winner over loser.
 * Votes and canonical pair uniqueness belong to one run (ADR-0003).
 */
@Entity
@Table(name = "matchups")
public class Matchup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MatchupRun run;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Song songA;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Song songB;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Song winner;

    @Column(nullable = false, length = 50)
    private String genre;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Matchup() {
        // JPA
    }

    public Matchup(MatchupRun run, Song songA, Song songB, Song winner) {
        this.run = run;
        this.user = run.getUser();
        this.songA = songA;
        this.songB = songB;
        this.winner = winner;
        this.genre = run.getGenre();
    }

    public Long getId() {
        return id;
    }

    public MatchupRun getRun() { return run; }

    public User getUser() {
        return user;
    }

    public Song getSongA() {
        return songA;
    }

    public Song getSongB() {
        return songB;
    }

    public Song getWinner() {
        return winner;
    }

    public String getGenre() {
        return genre;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
