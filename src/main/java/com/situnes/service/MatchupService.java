package com.situnes.service;

import com.situnes.domain.Song;
import com.situnes.domain.User;
import com.situnes.dto.MatchupPair;
import org.springframework.stereotype.Service;

/**
 * Runs the matchup arena: picks pairs, records votes, updates Elo ratings.
 * TODO: implement.
 *
 * Pairing strategy (your design decision): random pairs early, then Swiss-style
 * pairing of similarly-rated songs so later rounds are genuinely hard choices.
 */
@Service
public class MatchupService {

    /** Next head-to-head pair for this user in this genre. */
    public MatchupPair nextPair(User user, String genre) {
        throw new UnsupportedOperationException("TODO: pick two songs the user hasn't faced off yet");
    }

    /** Record the vote and update both songs' Elo ratings. */
    public void recordResult(User user, Long winnerSongId, Long loserSongId, String genre) {
        throw new UnsupportedOperationException("TODO: persist Matchup + update SongRatings via EloRatingService");
    }

    /** True once the user has completed rounds-per-genre for this genre. */
    public boolean isComplete(User user, String genre) {
        throw new UnsupportedOperationException("TODO: compare matchup count vs situnes.matchup.rounds-per-genre");
    }
}
