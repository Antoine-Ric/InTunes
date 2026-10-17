package com.situnes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * The named music profile produced after a user finishes a genre's matchup rounds.
 * archetypeKey (e.g. "HIPHOP_DEEP_90S") maps 1:1 to a Community.
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

    // TODO: getters/setters
}
