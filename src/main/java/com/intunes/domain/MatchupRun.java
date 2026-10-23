package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/** One attempt through a genre. Retakes retain the previous attempt's history (ADR-0003). */
@Entity
@Table(name = "matchup_runs")
public class MatchupRun {
    public enum Status { IN_PROGRESS, COMPLETED, ABANDONED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false, length = 50)
    private String genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.IN_PROGRESS;

    @Column(nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    private Instant finishedAt;

    @Version
    private long version;

    @Column(nullable = false)
    private int roundLimit = 16;

    protected MatchupRun() { }

    public MatchupRun(User user, String genre) {
        this(user, genre, 16);
    }

    public MatchupRun(User user, String genre, int roundLimit) {
        if (roundLimit < 1) throw new IllegalArgumentException("Round limit must be positive");
        this.user = Objects.requireNonNull(user, "user");
        this.genre = Objects.requireNonNull(genre, "genre");
        this.roundLimit = roundLimit;
    }

    public void complete() { finish(Status.COMPLETED); }
    public void abandon() { finish(Status.ABANDONED); }

    private void finish(Status terminalStatus) {
        if (status != Status.IN_PROGRESS) throw new IllegalStateException("Run is already finished");
        status = terminalStatus;
        finishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getGenre() { return genre; }
    public Status getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public long getVersion() { return version; }
    public int getRoundLimit() { return roundLimit; }
}
