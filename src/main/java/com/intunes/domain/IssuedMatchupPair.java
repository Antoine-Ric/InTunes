package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Server-side identity of a displayed pair. Refreshing never replaces an unanswered pair. */
@Entity
@Table(name = "issued_matchup_pairs")
public class IssuedMatchupPair {
    public enum Status { ISSUED, VOTED }

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MatchupRun run;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false, length = 50)
    private String genre;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "song_a_id")
    private Song songA;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "song_b_id")
    private Song songB;

    @Column(nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.ISSUED;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    private Instant resolvedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", unique = true)
    private Matchup vote;

    @Version
    private long version;

    protected IssuedMatchupPair() { }

    public IssuedMatchupPair(MatchupRun run, Song songA, Song songB, int roundNumber) {
        if (run.getStatus() != MatchupRun.Status.IN_PROGRESS || roundNumber < 1 || roundNumber > run.getRoundLimit()) {
            throw new IllegalArgumentException("Pair requires an active run and a valid round");
        }
        if (songA.getId() == null || songB.getId() == null || songA.getId().equals(songB.getId()) ||
                !run.getGenre().equals(songA.getGenre()) || !run.getGenre().equals(songB.getGenre())) {
            throw new IllegalArgumentException("Pair requires distinct persisted songs from the run's genre");
        }
        this.run = run;
        this.user = run.getUser();
        this.genre = run.getGenre();
        this.songA = songA;
        this.songB = songB;
        this.roundNumber = roundNumber;
    }

    /** Called in the same locked transaction as vote/Elo writes by the future voting service. */
    public void markVoted(Matchup vote) {
        boolean sameSongs = (songA.getId().equals(vote.getSongA().getId()) && songB.getId().equals(vote.getSongB().getId())) ||
                (songA.getId().equals(vote.getSongB().getId()) && songB.getId().equals(vote.getSongA().getId()));
        if (status != Status.ISSUED || vote.getId() == null || !Objects.equals(run.getId(), vote.getRun().getId()) || !sameSongs) {
            throw new IllegalArgumentException("Vote must resolve this issued pair in its own run exactly once");
        }
        this.vote = vote;
        status = Status.VOTED;
        resolvedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public MatchupRun getRun() { return run; }
    public User getUser() { return user; }
    public String getGenre() { return genre; }
    public Song getSongA() { return songA; }
    public Song getSongB() { return songB; }
    public int getRoundNumber() { return roundNumber; }
    public Status getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Matchup getVote() { return vote; }
    public long getVersion() { return version; }
}
