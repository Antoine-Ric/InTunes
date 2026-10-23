package com.intunes.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloRatingServiceTest {

    private static final double DELTA = 0.005;

    private final EloRatingService elo = new EloRatingService(32.0);

    @Test
    void evenMatchup_splitsKFactor() {
        // 1500 vs 1500, K=32 -> winner +16, loser -16
        assertArrayEquals(new double[]{1516.00, 1484.00}, elo.update(1500, 1500), DELTA);
    }

    @Test
    void underdogWin_gainsMoreThanFavoriteWould() {
        // 1400 beats 1700 -> big swing for the underdog
        assertArrayEquals(new double[]{1427.17, 1672.83}, elo.update(1400, 1700), DELTA);
    }

    @Test
    void favoriteWin_gainsLessThanUnderdogWould() {
        // 1700 beats 1400 -> small swing, the favorite was expected to win
        assertArrayEquals(new double[]{1704.83, 1395.17}, elo.update(1700, 1400), DELTA);
    }

    @Test
    void update_isZeroSum() {
        double winner = 1623.5, loser = 1488.25;
        double[] result = elo.update(winner, loser);
        // total rating conserved ...
        assertEquals(winner + loser, result[0] + result[1], 1e-9);
        // ... and the winner's gain equals the loser's loss
        assertEquals(result[0] - winner, loser - result[1], 1e-9);
    }

    @Test
    void customKFactor_scalesDeltas() {
        EloRatingService gentle = new EloRatingService(16.0);
        // half the K -> half the swing on an even matchup
        assertArrayEquals(new double[]{1508.00, 1492.00}, gentle.update(1500, 1500), DELTA);
    }

    @Test
    void fourHundredPointGap_isAbout91Percent() {
        assertEquals(0.9091, EloRatingService.expectedScore(1900, 1500), 0.0001);
    }

    @Test
    void underdogGainsMoreThanFavoriteForSamePairing() {
        double[] underdogWin = elo.update(1400, 1700);
        double[] favoriteWin = elo.update(1700, 1400);
        assertTrue(underdogWin[0] - 1400 > favoriteWin[0] - 1700);
    }

    @ParameterizedTest
    @CsvSource({
            "1500, 1500, 0.5",
            "1900, 1500, 0.9090909090909091",
            "1500, 1900, 0.09090909090909091",
            "2300, 1500, 0.9900990099009901",
            "1500, 2300, 0.009900990099009901"
    })
    void expectedScores_followLogisticCurveAndSumToOne(double ratingA, double ratingB,
                                                      double expected) {
        double scoreA = EloRatingService.expectedScore(ratingA, ratingB);
        double scoreB = EloRatingService.expectedScore(ratingB, ratingA);

        assertEquals(expected, scoreA, 1e-12);
        assertEquals(1.0, scoreA + scoreB, 1e-12);
    }

    @Test
    void update_preservesFractionalPrecision() {
        // A 400-point favorite gains exactly K/11; retain all precision internally.
        double gain = 32.0 / 11.0;
        assertArrayEquals(new double[]{1900.25 + gain, 1500.25 - gain},
                elo.update(1900.25, 1500.25), 1e-12);
    }

    @Test
    void zeroKFactor_leavesRatingsUnchanged() {
        EloRatingService frozen = new EloRatingService(0.0);
        assertArrayEquals(new double[]{1423.75, 1786.125},
                frozen.update(1423.75, 1786.125), 0.0);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "1623.5, 1488.25",
            "-400.25, 200.75",
            "1000000, -1000000",
            "-1000000, 1000000"
    })
    void update_keepsChangesFiniteBoundedAndZeroSum(double winner, double loser) {
        double[] result = elo.update(winner, loser);
        double gain = result[0] - winner;
        double loss = loser - result[1];

        assertTrue(Double.isFinite(result[0]));
        assertTrue(Double.isFinite(result[1]));
        assertTrue(gain >= 0.0 && gain <= 32.0);
        assertTrue(loss >= 0.0 && loss <= 32.0);
        assertEquals(gain, loss, 1e-9);
        assertEquals(winner + loser, result[0] + result[1], 1e-9);
    }

    @Test
    void shiftingBothRatings_preservesRatingChanges() {
        double[] original = elo.update(1423.75, 1786.125);
        double[] shifted = elo.update(2423.75, 2786.125);

        assertArrayEquals(new double[]{original[0] + 1000, original[1] + 1000},
                shifted, 1e-9);
    }

    @Test
    void repeatedWins_haveDiminishingGainsAndConserveTotalRating() {
        double winner = 1500.0;
        double loser = 1500.0;
        double previousGain = Double.POSITIVE_INFINITY;

        for (int round = 0; round < 1000; round++) {
            double[] result = elo.update(winner, loser);
            double gain = result[0] - winner;

            assertTrue(gain > 0.0 && gain < previousGain,
                    "Winner's gain should diminish at round " + round);
            assertEquals(3000.0, result[0] + result[1], 1e-9);

            winner = result[0];
            loser = result[1];
            previousGain = gain;
        }
    }
}
