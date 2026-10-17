package com.situnes.dto;

/** One head-to-head round: two songs, pick a winner. */
public record MatchupPair(
        SongDto songA,
        SongDto songB,
        int round,
        int totalRounds
) {
}
