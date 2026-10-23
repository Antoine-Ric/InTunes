package com.intunes.dto;

/** Track metadata returned by the Spotify Web API search. */
public record SpotifyTrack(
        String spotifyId,
        String title,
        String artist,
        String albumArtUrl,
        Integer popularity,
        Integer releaseYear,
        String isrc
) {
}
