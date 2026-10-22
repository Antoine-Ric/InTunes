package com.intunes.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * iTunes Search API — our 30-second preview source. Free, no auth, no key.
 *   GET {base-url}/search?term={artist title}&entity=song&limit=5
 * Response field: previewUrl.
 *
 * <p>Returns null when iTunes has no match — a Song's previewUrl is
 * nullable for exactly this reason, and the arena must handle it.
 */
@Component
public class ITunesClient {

    private final String baseUrl;
    private final RestClient restClient;

    public ITunesClient(@Value("${intunes.itunes.base-url}") String baseUrl) {
        this(baseUrl, RestClient.builder().build());
    }

    ITunesClient(String baseUrl, RestClient restClient) {
        this.baseUrl = baseUrl;
        this.restClient = restClient;
    }

    /** Returns the 30s preview URL, or null when iTunes has no match. */
    public String fetchPreviewUrl(String title, String artist) {
        SearchResponse response = restClient.get()
                .uri(baseUrl + "/search?term={term}&entity=song&limit=5",
                        artist + " " + title)
                .retrieve()
                .body(SearchResponse.class);
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return null;
        }
        // Prefer the result that actually matches the song we asked for:
        // the first hit for a popular title is often a karaoke or
        // tribute version, and its preview would be the wrong audio.
        for (Result result : response.results()) {
            if (matches(result, title, artist)) {
                return result.previewUrl();
            }
        }
        return response.results().get(0).previewUrl();
    }

    private static boolean matches(Result result, String title, String artist) {
        return containsIgnoreCase(result.trackName(), title)
                && containsIgnoreCase(result.artistName(), artist);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null
                && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    // --- iTunes wire payload (only the fields we read) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(List<Result> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(String trackName, String artistName, String previewUrl) {
    }
}
