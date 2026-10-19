package com.situnes.service;

import org.junit.jupiter.api.Test;

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
}
