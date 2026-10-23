package com.intunes.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** A community board post. */
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Community community;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User author;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Post() {
        // JPA
    }

    public Post(Community community, User author, String body) {
        this.community = community;
        this.author = author;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public Community getCommunity() {
        return community;
    }

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
