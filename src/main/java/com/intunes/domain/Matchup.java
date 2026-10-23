package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One head-to-head vote: the user picked winner over loser.
 * TODO (ADR-0003): add run identity before implementing vote persistence.
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

    public Matchup(User user, Song songA, Song songB, Song winner, String genre) {
        this.user = user;
        this.songA = songA;
        this.songB = songB;
        this.winner = winner;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

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
