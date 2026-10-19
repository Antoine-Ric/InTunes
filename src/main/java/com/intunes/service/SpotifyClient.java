package com.intunes.service;

import com.intunes.config.SpotifyConfig;
import org.springframework.stereotype.Component;

/**
 * Spotify Web API — metadata only (client-credentials flow, no user login).
 * Gives us: track name, artist, album art, popularity, release date, ISRC.
 *
 * WARNING: audio-features, audio-analysis, recommendations, and preview_url
 * are DEAD for new apps (Spotify killed them Nov 27, 2024). Do not build on them.
 *
 * TODO: implement token exchange (POST accounts.spotify.com/api/token) and
 * GET /v1/search + /v1/tracks. Cache the token until expiry.
 */
@Component
public class SpotifyClient {

    private final SpotifyConfig config;

    public SpotifyClient(SpotifyConfig config) {
        this.config = config;
    }
}
