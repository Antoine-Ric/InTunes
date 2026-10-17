package com.situnes.dto;

/** What the matchup arena needs to render one song card. */
public record SongDto(
        Long id,
        String title,
        String artist,
        String albumArtUrl,
        String previewUrl
) {
}
