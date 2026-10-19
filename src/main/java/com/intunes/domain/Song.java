package com.intunes.domain;

import jakarta.persistence.*;

/**
 * A seed song available for matchups. Metadata comes from the Spotify API,
 * the 30s preview comes from the iTunes Search API. Cached in our DB so the
 * matchup arena never depends on a live third-party call.
 */
@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 64)
    private String spotifyId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(length = 512)
    private String albumArtUrl;

    /** 30-second preview URL (iTunes). Null when unavailable — arena must handle it. */
    @Column(length = 512)
    private String previewUrl;

    /** Curated genre bucket this song competes in, e.g. "hiphop", "rnb", "afrobeats". */
    @Column(nullable = false, length = 50)
    private String genre;

    /** Spotify popularity 0-100. Feeds the archetype matrix (mainstream vs deep-cut). */
    private Integer popularity;

    /** Release year. Feeds the archetype matrix (era). */
    private Integer releaseYear;

    // TODO: getters/setters
}
