package com.situnes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Pure Elo math for head-to-head song matchups.
 * TODO: implement update() — expected score, K-factor applied, return new ratings.
 * Formula: expectedA = 1 / (1 + 10^((ratingB - ratingA)/400))
 *           newA = ratingA + K * (scoreA - expectedA), scoreA = 1 on win, 0 on loss.
 */
@Service
public class EloRatingService {

    private final double kFactor;

    public EloRatingService(@Value("${situnes.matchup.elo-k-factor:32}") double kFactor) {
        this.kFactor = kFactor;
    }

    /** Returns {newRatingWinner, newRatingLoser}. */
    public double[] update(double winnerRating, double loserRating) {
        throw new UnsupportedOperationException("TODO: implement Elo update");
    }
}
