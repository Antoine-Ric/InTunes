package com.situnes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** One head-to-head vote: the user picked winner over loser. */
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

    // TODO: getters/setters
}
