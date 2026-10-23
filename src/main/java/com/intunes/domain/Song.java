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

    protected Song() {
        // JPA
    }

    public Song(String spotifyId, String title, String artist, String albumArtUrl,
                String previewUrl, String genre, Integer popularity, Integer releaseYear) {
        this.spotifyId = spotifyId;
        this.title = title;
        this.artist = artist;
        this.albumArtUrl = albumArtUrl;
        this.previewUrl = previewUrl;
        this.genre = genre;
        this.popularity = popularity;
        this.releaseYear = releaseYear;
    }

    public Long getId() {
        return id;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumArtUrl() {
        return albumArtUrl;
    }

    public void setAlbumArtUrl(String albumArtUrl) {
        this.albumArtUrl = albumArtUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }
}
