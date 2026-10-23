package com.intunes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ITunesClientTest {

    private MockRestServiceServer server;
    private ITunesClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ITunesClient("https://itunes.apple.com", builder.build());
    }

    private void expectSearch(String responseBody) {
        server.expect(once(), requestTo(containsString("https://itunes.apple.com/search")))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    @Test
    void fetchPreviewUrl_prefersTheActualMatchOverKaraoke() {
        expectSearch("""
                {"resultCount":2,"results":[
                  {"trackName":"Song One (Karaoke Version)","artistName":"Karaoke Kings",
                   "previewUrl":"https://prev/karaoke.m4a"},
                  {"trackName":"Song One","artistName":"Artist One",
                   "previewUrl":"https://prev/real.m4a"}
                ]}
                """);

        assertEquals("https://prev/real.m4a", client.fetchPreviewUrl("Song One", "Artist One"));
        server.verify();
    }

    @Test
    void fetchPreviewUrl_fallsBackToFirstResultWhenNothingMatches() {
        expectSearch("""
                {"resultCount":1,"results":[
                  {"trackName":"Some Other Song","artistName":"Someone Else",
                   "previewUrl":"https://prev/first.m4a"}
                ]}
                """);

        assertEquals("https://prev/first.m4a", client.fetchPreviewUrl("Song One", "Artist One"));
        server.verify();
    }

    @Test
    void fetchPreviewUrl_nullWhenNoResults() {
        expectSearch("{\"resultCount\":0,\"results\":[]}");

        assertNull(client.fetchPreviewUrl("Nope", "Nobody"));
        server.verify();
    }
}
