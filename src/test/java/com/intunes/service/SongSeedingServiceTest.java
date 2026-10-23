package com.intunes.service;

import com.intunes.domain.Song;
import com.intunes.dto.SeedTrack;
import com.intunes.dto.SpotifyTrack;
import com.intunes.repository.SongRepository;
import com.intunes.service.SongSeedingService.SeedReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongSeedingServiceTest {

    @Mock
    private SpotifyClient spotifyClient;
    @Mock
    private ITunesClient iTunesClient;
    @Mock
    private LastFmClient lastFmClient;
    @Mock
    private SongRepository songRepository;

    private SongSeedingService service;

    @BeforeEach
    void setUp() {
        service = new SongSeedingService(spotifyClient, iTunesClient, lastFmClient, songRepository);
    }

    private static SpotifyTrack spotifyTrack() {
        return new SpotifyTrack("abc123", "Song One", "Artist One",
                "https://img/large.jpg", 73, 2019, "USXXX1900001");
    }

    @Test
    void seedGenre_createsSongWithEnrichedFields() {
        when(songRepository.existsByTitleIgnoreCaseAndArtistIgnoreCaseAndGenre(
                "Song One", "Artist One", "rnb")).thenReturn(false);
        when(spotifyClient.searchTrack("Song One", "Artist One"))
                .thenReturn(Optional.of(spotifyTrack()));
        when(songRepository.findBySpotifyId("abc123")).thenReturn(Optional.empty());
        when(iTunesClient.fetchPreviewUrl("Song One", "Artist One"))
                .thenReturn("https://prev/real.m4a");

        SeedReport report = service.seedGenre("rnb",
                List.of(new SeedTrack("Song One", "Artist One")));

        assertEquals(new SeedReport(1, 0, 0), report);
        ArgumentCaptor<Song> captor = ArgumentCaptor.forClass(Song.class);
        verify(songRepository).save(captor.capture());
        Song saved = captor.getValue();
        assertEquals("abc123", saved.getSpotifyId());
        assertEquals("Song One", saved.getTitle());
        assertEquals("Artist One", saved.getArtist());
        assertEquals("https://img/large.jpg", saved.getAlbumArtUrl());
        assertEquals("https://prev/real.m4a", saved.getPreviewUrl());
        assertEquals("rnb", saved.getGenre());
        assertEquals(73, saved.getPopularity());
        assertEquals(2019, saved.getReleaseYear());
    }

    @Test
    void seedGenre_skipsSongAlreadyInThePool() {
        when(songRepository.existsByTitleIgnoreCaseAndArtistIgnoreCaseAndGenre(
                anyString(), anyString(), eq("rnb"))).thenReturn(true);

        SeedReport report = service.seedGenre("rnb",
                List.of(new SeedTrack("Song One", "Artist One")));

        assertEquals(new SeedReport(0, 1, 0), report);
        verifyNoInteractions(spotifyClient);
        verify(songRepository, never()).save(any());
    }

    @Test
    void seedGenre_skipsWhenSpotifyIdAlreadySeededUnderAnotherSpelling() {
        when(songRepository.existsByTitleIgnoreCaseAndArtistIgnoreCaseAndGenre(
                anyString(), anyString(), anyString())).thenReturn(false);
        when(spotifyClient.searchTrack(anyString(), anyString()))
                .thenReturn(Optional.of(spotifyTrack()));
        when(songRepository.findBySpotifyId("abc123"))
                .thenReturn(Optional.of(new Song("abc123", "Song One", "Artist One",
                        null, null, "rnb", 73, 2019)));

        SeedReport report = service.seedGenre("rnb",
                List.of(new SeedTrack("Song One", "Artist One")));

        assertEquals(new SeedReport(0, 1, 0), report);
        verify(songRepository, never()).save(any());
    }

    @Test
    void seedGenre_countsSpotifyMissAsFailureAndContinues() {
        SeedTrack miss = new SeedTrack("Ghost Track", "Ghost Artist");
        SeedTrack hit = new SeedTrack("Song One", "Artist One");
        when(songRepository.existsByTitleIgnoreCaseAndArtistIgnoreCaseAndGenre(
                anyString(), anyString(), eq("rnb"))).thenReturn(false);
        when(spotifyClient.searchTrack("Ghost Track", "Ghost Artist")).thenReturn(Optional.empty());
        when(spotifyClient.searchTrack("Song One", "Artist One"))
                .thenReturn(Optional.of(spotifyTrack()));
        when(songRepository.findBySpotifyId("abc123")).thenReturn(Optional.empty());
        when(iTunesClient.fetchPreviewUrl(anyString(), anyString())).thenReturn(null);

        SeedReport report = service.seedGenre("rnb", List.of(miss, hit));

        assertEquals(new SeedReport(1, 0, 1), report);
        verify(songRepository).save(any());
    }

    @Test
    void seedFromTag_draftsFromLastFmThenSeeds() {
        List<SeedTrack> draft = List.of(new SeedTrack("Song One", "Artist One"));
        when(lastFmClient.topTracksForTag("rnb", 25)).thenReturn(draft);
        when(songRepository.existsByTitleIgnoreCaseAndArtistIgnoreCaseAndGenre(
                anyString(), anyString(), anyString())).thenReturn(true);

        SeedReport report = service.seedFromTag("rnb", "rnb", 25);

        assertEquals(new SeedReport(0, 1, 0), report);
        verify(lastFmClient).topTracksForTag("rnb", 25);
    }
}
