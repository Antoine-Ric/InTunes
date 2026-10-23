package com.intunes.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intunes.config.LastFmConfig;
import com.intunes.dto.SeedTrack;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Last.fm API — genre tag charts. tag.gettoptracks DRAFTS seed lists
 * (top tracks for a tag like "rnb" or "hip-hop") so nobody has to type
 * 125 songs by hand; the draft still gets curated before seeding.
 *
 * <p>Read-only usage: only the API key is needed, never the shared secret.
 */
@Component
public class LastFmClient {

    static final String DEFAULT_BASE_URL = "https://ws.audioscrobbler.com/2.0";

    private final LastFmConfig config;
    private final RestClient restClient;

    public LastFmClient(LastFmConfig config) {
        this(config, RestClient.builder().build());
    }

    LastFmClient(LastFmConfig config, RestClient restClient) {
        this.config = config;
        this.restClient = restClient;
    }

    /** Top tracks for a genre tag, as (title, artist) seed candidates. */
    public List<SeedTrack> topTracksForTag(String tag, int limit) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "Last.fm API key is not configured (set LASTFM_API_KEY / intunes.lastfm.api-key)");
        }
        TopTracksResponse response = restClient.get()
                .uri(baseUrl() + "/?method=tag.gettoptracks&tag={tag}&limit={limit}&api_key={key}&format=json",
                        tag, limit, config.apiKey())
                .retrieve()
                .body(TopTracksResponse.class);
        if (response == null || response.tracks() == null || response.tracks().track() == null) {
            return List.of();
        }
        return response.tracks().track().stream()
                .map(t -> new SeedTrack(t.name(), t.artist() == null ? null : t.artist().name()))
                .toList();
    }

    private String baseUrl() {
        String base = config.baseUrl();
        if (base == null || base.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    // --- Last.fm wire payload (only the fields we read) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TopTracksResponse(TrackList tracks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrackList(List<TrackEntry> track) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrackEntry(String name, ArtistRef artist) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArtistRef(String name) {
    }
}
