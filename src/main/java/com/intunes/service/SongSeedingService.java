package com.intunes.service;

import com.intunes.domain.Song;
import com.intunes.dto.SeedTrack;
import com.intunes.dto.SpotifyTrack;
import com.intunes.repository.SongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Turns (title, artist) candidates into Song rows: Spotify supplies the
 * metadata, iTunes the 30-second preview. Idempotent by design — seeding
 * the same genre twice creates no duplicates, so it is safe to re-run
 * after editing a curated list.
 *
 * <p>One bad candidate never sinks the batch: it is counted as failed and
 * the rest still seed.
 */
@Service
public class SongSeedingService {

    private static final Logger log = LoggerFactory.getLogger(SongSeedingService.class);

    private final SpotifyClient spotifyClient;
    private final ITunesClient iTunesClient;
    private final LastFmClient lastFmClient;
    private final SongRepository songRepository;

    public SongSeedingService(SpotifyClient spotifyClient, ITunesClient iTunesClient,
                              LastFmClient lastFmClient, SongRepository songRepository) {
        this.spotifyClient = spotifyClient;
        this.iTunesClient = iTunesClient;
        this.lastFmClient = lastFmClient;
        this.songRepository = songRepository;
    }

    /** Outcome counts for one seeding run. */
    public record SeedReport(int created, int skipped, int failed) {
    }

    /** Draft a seed list for a genre from Last.fm tag charts, for curation. */
    public List<SeedTrack> draftSeedList(String tag, int limit) {
        return lastFmClient.topTracksForTag(tag, limit);
    }

    /**
     * Seeds one genre from a curated candidate list. Candidates already in
     * the pool (same title + artist + genre, or same Spotify id under a
     * different spelling) are skipped.
     */
    public SeedReport seedGenre(String genre, List<SeedTrack> candidates) {
        int created = 0;
        int skipped = 0;
        int failed = 0;
        for (SeedTrack candidate : candidates) {
            try {
                if (seedOne(genre, candidate)) {
                    created++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("Seeding failed for {} - {} [{}]: {}",
                        candidate.artist(), candidate.title(), genre, e.getMessage());
            }
        }
        log.info("Seeded genre {}: {} created, {} skipped, {} failed", genre, created, skipped, failed);
        return new SeedReport(created, skipped, failed);
    }

    /** Draft from a Last.fm tag and seed in one call. */
    public SeedReport seedFromTag(String genre, String tag, int limit) {
        return seedGenre(genre, draftSeedList(tag, limit));
    }

    /** @return true when a new Song was created, false when it already existed */
    private boolean seedOne(String genre, SeedTrack candidate) {
        if (songRepository.existsByTitleIgnoreCaseAndArtistIgnoreCaseAndGenre(
                candidate.title(), candidate.artist(), genre)) {
            return false;
        }
        Optional<SpotifyTrack> match = spotifyClient.searchTrack(candidate.title(), candidate.artist());
        if (match.isEmpty()) {
            throw new IllegalStateException("No Spotify match");
        }
        SpotifyTrack track = match.get();
        if (track.spotifyId() != null && songRepository.findBySpotifyId(track.spotifyId()).isPresent()) {
            return false;
        }
        String previewUrl = iTunesClient.fetchPreviewUrl(track.title(), track.artist());
        Song song = new Song(track.spotifyId(), track.title(), track.artist(),
                track.albumArtUrl(), previewUrl, genre, track.popularity(), track.releaseYear());
        songRepository.save(song);
        return true;
    }
}
