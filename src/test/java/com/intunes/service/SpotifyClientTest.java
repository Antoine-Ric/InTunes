package com.intunes.service;

import com.intunes.config.SpotifyConfig;
import com.intunes.dto.SpotifyTrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SpotifyClientTest {

    private MockRestServiceServer server;
    private SpotifyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SpotifyClient(new SpotifyConfig("test-id", "test-secret"), builder.build());
    }

    private void expectTokenRequest() {
        server.expect(once(), requestTo("https://accounts.spotify.com/api/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-123\",\"token_type\":\"Bearer\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));
    }

    private void expectSearchRequest(String responseBody) {
        server.expect(once(), requestTo(containsString("https://api.spotify.com/v1/search")))
                .andExpect(header("Authorization", "Bearer tok-123"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    @Test
    void searchTrack_mapsSpotifyFields() {
        expectTokenRequest();
        expectSearchRequest("""
                {"tracks":{"items":[{
                  "id":"abc123",
                  "name":"Song One",
                  "artists":[{"name":"Artist One"}],
                  "album":{"release_date":"2019-05-17",
                           "images":[{"url":"https://img/large.jpg"},{"url":"https://img/small.jpg"}]},
                  "popularity":73,
                  "external_ids":{"isrc":"USXXX1900001"}
                }]}}
                """);

        Optional<SpotifyTrack> result = client.searchTrack("Song One", "Artist One");

        assertTrue(result.isPresent());
        SpotifyTrack track = result.get();
        assertEquals("abc123", track.spotifyId());
        assertEquals("Song One", track.title());
        assertEquals("Artist One", track.artist());
        assertEquals("https://img/large.jpg", track.albumArtUrl());
        assertEquals(73, track.popularity());
        assertEquals(2019, track.releaseYear());
        assertEquals("USXXX1900001", track.isrc());
        server.verify();
    }

    @Test
    void searchTrack_emptyWhenSpotifyHasNoMatch() {
        expectTokenRequest();
        expectSearchRequest("{\"tracks\":{\"items\":[]}}");

        assertTrue(client.searchTrack("Nope", "Nobody").isEmpty());
        server.verify();
    }

    @Test
    void accessToken_isCachedAcrossSearches() {
        // One token expectation for two searches: if the client refetched,
        // MockRestServiceServer would fail the second token call.
        expectTokenRequest();
        expectSearchRequest("{\"tracks\":{\"items\":[]}}");
        expectSearchRequest("{\"tracks\":{\"items\":[]}}");

        client.searchTrack("One", "A");
        client.searchTrack("Two", "B");

        server.verify();
    }
}
