package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * The named music profile produced after a user finishes a genre's matchup rounds.
 * archetypeKey (e.g. "HIPHOP_DEEP_90S") maps 1:1 to a Community.
 * ADR-0003: update this user/genre profile in place from the latest completed
 * run only. The profile retains its identity when a retake replaces its result.
 */
@Entity
@Table(name = "taste_profiles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "genre"}))
public class TasteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MatchupRun run;

    @Version
    private long version;

    @Column(nullable = false, length = 50)
    private String genre;

    /** Machine key, e.g. "HIPHOP_DEEP_90S". One per Community. */
    @Column(nullable = false, length = 100)
    private String archetypeKey;

    /** Human name, e.g. "Boom Bap Archaeologist". */
    @Column(nullable = false, length = 100)
    private String archetypeName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TasteProfile() {
        // JPA
    }

    public TasteProfile(MatchupRun run, String archetypeKey, String archetypeName) {
        this.user = run.getUser();
        this.genre = run.getGenre();
        updateFromRun(run, archetypeKey, archetypeName);
    }

    public void updateFromRun(MatchupRun run, String archetypeKey, String archetypeName) {
        if (run.getStatus() != MatchupRun.Status.COMPLETED) {
            throw new IllegalArgumentException("Profile requires a completed run");
        }
        if (!genre.equals(run.getGenre()) ||
                (user != run.getUser() && (user.getId() == null ||
                        !java.util.Objects.equals(user.getId(), run.getUser().getId())))) {
            throw new IllegalArgumentException("Profile owner and genre cannot change");
        }
        this.run = run;
        this.archetypeKey = archetypeKey;
        this.archetypeName = archetypeName;
    }

    public Long getId() {
        return id;
    }

    public MatchupRun getRun() { return run; }
    public long getVersion() { return version; }

    public User getUser() {
        return user;
    }

    public String getGenre() {
        return genre;
    }

    public String getArchetypeKey() {
        return archetypeKey;
    }

    public void setArchetypeKey(String archetypeKey) {
        this.archetypeKey = archetypeKey;
    }

    public String getArchetypeName() {
        return archetypeName;
    }

    public void setArchetypeName(String archetypeName) {
        this.archetypeName = archetypeName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
