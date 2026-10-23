package com.intunes.domain;

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

    // Member count is derived from profiles, rather than stored here.

    protected Community() {
        // JPA
    }

    public Community(String archetypeKey, String name, String description) {
        this.archetypeKey = archetypeKey;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getArchetypeKey() {
        return archetypeKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
