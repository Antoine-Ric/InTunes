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

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Reply() {
        // JPA
    }

    public Reply(Post post, User author, String body) {
        this.post = post;
        this.author = author;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() { return version; }

    public Post getPost() {
        return post;
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

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
