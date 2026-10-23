package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * The named music profile produced after a user finishes a genre's matchup rounds.
 * archetypeKey (e.g. "HIPHOP_DEEP_90S") maps 1:1 to a Community.
 * ADR-0003: update this user/genre profile in place from the latest completed
 * run only; run-scoped rating queries are a prerequisite for profile building.
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

    public TasteProfile(User user, String genre, String archetypeKey, String archetypeName) {
        this.user = user;
        this.genre = genre;
        this.archetypeKey = archetypeKey;
        this.archetypeName = archetypeName;
    }

    public Long getId() {
        return id;
    }

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
