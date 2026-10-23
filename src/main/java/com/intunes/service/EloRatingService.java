package com.intunes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Pure Elo math for head-to-head song matchups.
 *
 * <p>Zero-sum: the winner's gain always equals the loser's loss, so the sum of
 * all ratings in a genre pool stays constant (n x 1500 for fresh songs).
 * No rounding happens inside the calculation; round only for display.
 */
@Service
public class EloRatingService {

    private final double kFactor;

    public EloRatingService(@Value("${intunes.matchup.elo-k-factor:32}") double kFactor) {
        this.kFactor = kFactor;
    }

    /**
     * Applies one matchup result.
     *
     * @param winnerRating rating of the song that won the vote
     * @param loserRating  rating of the song that lost the vote
     * @return {newWinnerRating, newLoserRating}
     */
    public double[] update(double winnerRating, double loserRating) {
        double expectedWinner = expectedScore(winnerRating, loserRating);
        double winnerGain = kFactor * (1.0 - expectedWinner);
        return new double[]{winnerRating + winnerGain, loserRating - winnerGain};
    }

    /**
     * Expected score of song A against song B on the base-10 logistic curve.
     * A 400-point gap works out to about a 91% win expectancy.
     */
    static double expectedScore(double ratingA, double ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }
}
