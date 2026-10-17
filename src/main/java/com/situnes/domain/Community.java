package com.situnes.domain;

import jakarta.persistence.*;

/** A peer group for one archetype. Users land here when their profile resolves. */
@Entity
@Table(name = "communities")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Matches TasteProfile.archetypeKey, e.g. "HIPHOP_DEEP_90S". */
    @Column(nullable = false, unique = true, length = 100)
    private String archetypeKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // TODO: getters/setters. Member count can be derived (count profiles) — no column needed for MVP.
}
