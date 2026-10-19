package com.intunes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * iTunes Search API — our 30-second preview source. Free, no auth, no key.
 *   GET {base-url}/search?term={artist - title}&entity=song&limit=5
 * Match on ISRC when Spotify gives us one; fall back to title+artist.
 * Response field: previewUrl.
 *
 * TODO: implement lookup with a small in-memory/DB cache — the seeding
 * pipeline will call this hundreds of times and Apple rate-limits.
 */
@Component
public class ITunesClient {

    private final String baseUrl;

    public ITunesClient(@Value("${intunes.itunes.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** Returns the 30s preview URL, or null when iTunes has no match. */
    public String fetchPreviewUrl(String title, String artist) {
        throw new UnsupportedOperationException("TODO: call iTunes Search API");
    }
}
