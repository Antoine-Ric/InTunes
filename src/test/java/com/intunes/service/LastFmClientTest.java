package com.intunes.service;

import com.intunes.config.LastFmConfig;
import com.intunes.dto.SeedTrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LastFmClientTest {

    private MockRestServiceServer server;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void topTracksForTag_mapsToSeedTracks() {
        // baseUrl left null on purpose: the client must use the default.
        LastFmClient client = new LastFmClient(
                new LastFmConfig("key-123", "secret-123", null), builder.build());
        server.expect(once(), requestTo(containsString("https://ws.audioscrobbler.com/2.0/")))
                .andExpect(requestTo(containsString("method=tag.gettoptracks")))
                .andRespond(withSuccess("""
                        {"tracks":{"track":[
                          {"name":"Track A","artist":{"name":"Artist A"}},
                          {"name":"Track B","artist":{"name":"Artist B"}}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        List<SeedTrack> result = client.topTracksForTag("rnb", 25);

        assertEquals(List.of(new SeedTrack("Track A", "Artist A"),
                new SeedTrack("Track B", "Artist B")), result);
        server.verify();
    }

    @Test
    void topTracksForTag_blankApiKeyFailsFast() {
        LastFmClient client = new LastFmClient(
                new LastFmConfig(" ", "secret-123", null), builder.build());

        assertThrows(IllegalStateException.class, () -> client.topTracksForTag("rnb", 25));
    }
}
