package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A reply on a post. When pinned=true, it also renders on the author's
 * public music profile for visitors to see.
 */
@Entity
@Table(name = "replies")
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Post post;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User author;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // TODO: getters/setters
}
