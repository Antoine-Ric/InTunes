package com.intunes.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intunes.config.SpotifyConfig;
import com.intunes.dto.SpotifyTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Spotify Web API — metadata only (client-credentials flow, no user login).
 * Gives us: track name, artist, album art, popularity, release date, ISRC.
 *
 * <p>WARNING: audio-features, audio-analysis, recommendations, and preview_url
 * are DEAD for new apps (Spotify killed them Nov 27, 2024). Do not build on them.
 *
 * <p>The access token is fetched lazily and cached until shortly before it
 * expires. Spotify tokens last an hour, so refetching on every search would
 * waste calls and flirt with auth rate limits during a seeding run.
 */
@Component
public class SpotifyClient {

    private static final Logger log = LoggerFactory.getLogger(SpotifyClient.class);

    static final String AUTH_URL = "https://accounts.spotify.com/api/token";
    static final String API_BASE_URL = "https://api.spotify.com";
    /** Refresh the token this many seconds before Spotify says it expires. */
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;

    private final SpotifyConfig config;
    private final RestClient restClient;

    private volatile String accessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public SpotifyClient(SpotifyConfig config) {
        this(config, RestClient.builder().build());
    }

    SpotifyClient(SpotifyConfig config, RestClient restClient) {
        this.config = config;
        this.restClient = restClient;
    }

    /**
     * Best Spotify match for a title + artist pair, or empty when Spotify
     * has nothing. Release year comes from the album release date.
     */
    public Optional<SpotifyTrack> searchTrack(String title, String artist) {
        SearchResponse response = restClient.get()
                .uri(API_BASE_URL + "/v1/search?q={q}&type=track&limit=1",
                        "track:\"" + title + "\" artist:\"" + artist + "\"")
                .header("Authorization", "Bearer " + accessToken())
                .retrieve()
                .body(SearchResponse.class);
        if (response == null || response.tracks() == null
                || response.tracks().items() == null || response.tracks().items().isEmpty()) {
            return Optional.empty();
        }
        TrackItem item = response.tracks().items().get(0);
        return Optional.of(new SpotifyTrack(
                item.id(),
                item.name(),
                item.artists() == null || item.artists().isEmpty() ? artist : item.artists().get(0).name(),
                item.album() == null || item.album().images() == null || item.album().images().isEmpty()
                        ? null : item.album().images().get(0).url(),
                item.popularity(),
                parseReleaseYear(item.album() == null ? null : item.album().releaseDate()),
                item.externalIds() == null ? null : item.externalIds().isrc()));
    }

    /** Cached token; fetches a new one only when missing or about to expire. */
    synchronized String accessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }
        String credentials = Base64.getEncoder().encodeToString(
                (config.clientId() + ":" + config.clientSecret()).getBytes(StandardCharsets.UTF_8));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        TokenResponse token = restClient.post()
                .uri(AUTH_URL)
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken() == null) {
            throw new IllegalStateException("Spotify token request returned no access_token");
        }
        accessToken = token.accessToken();
        long usableSeconds = Math.max(0, token.expiresIn() - EXPIRY_SAFETY_MARGIN_SECONDS);
        tokenExpiresAt = Instant.now().plusSeconds(usableSeconds);
        log.debug("Fetched Spotify access token, expires in {}s", token.expiresIn());
        return accessToken;
    }

    private static Integer parseReleaseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // --- Spotify wire payloads (only the fields we read) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(TracksPage tracks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TracksPage(List<TrackItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TrackItem(
            String id,
            String name,
            List<ArtistRef> artists,
            AlbumRef album,
            Integer popularity,
            @JsonProperty("external_ids") ExternalIds externalIds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ArtistRef(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AlbumRef(
            @JsonProperty("release_date") String releaseDate,
            List<ImageRef> images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ImageRef(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExternalIds(String isrc) {
    }
}
