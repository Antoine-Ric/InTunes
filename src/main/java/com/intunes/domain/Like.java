package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/** A like on either a post or a reply (exactly one must be set). */
@Entity
@Table(name = "likes")
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    private Reply reply;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // V2 also enforces exactly one target and unique user/target likes in PostgreSQL.

    protected Like() {
        // JPA
    }

    public Like(User user, Post post) {
        this.user = Objects.requireNonNull(user, "user");
        this.post = Objects.requireNonNull(post, "post");
    }

    public Like(User user, Reply reply) {
        this.user = Objects.requireNonNull(user, "user");
        this.reply = Objects.requireNonNull(reply, "reply");
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Post getPost() {
        return post;
    }

    public Reply getReply() {
        return reply;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
