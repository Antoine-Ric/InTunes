package com.intunes.service;

import com.intunes.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class PairingPolicyTest {
    private final PairingPolicy policy = new PairingPolicy();
    private final MatchupRun run = new MatchupRun(new User("listener", "encoded", null), "rnb");

    @Test
    void earlyRoundsRandomizeAmongLeastExposedSongs() {
        List<Song> pool = pool(8);
        List<Matchup> history = new ArrayList<>();
        Random random = new Random(42);
        Set<Long> seen = new HashSet<>();
        for (int round = 1; round <= 4; round++) {
            PairingPolicy.Pair pair = policy.select(pool, history, List.of(), round, 1500, random).orElseThrow();
            assertThat(seen.add(pair.songA().getId())).isTrue();
            assertThat(seen.add(pair.songB().getId())).isTrue();
            history.add(new Matchup(run, pair.songA(), pair.songB(), pair.songA()));
        }
        assertThat(seen).hasSize(8);
    }

    @Test
    void swissPairsLeastExposedAnchorWithNearestUnusedRating() {
        List<Song> pool = pool(6);
        List<Matchup> history = List.of(vote(pool, 1, 3), vote(pool, 2, 4), vote(pool, 5, 6), vote(pool, 3, 4));
        List<SongRating> ratings = List.of(rating(pool, 1, 1000), rating(pool, 2, 1020),
                rating(pool, 3, 1400), rating(pool, 4, 1420), rating(pool, 5, 1800), rating(pool, 6, 1820));
        PairingPolicy.Pair pair = policy.select(pool, history, ratings, 5, 1500).orElseThrow();
        assertThat(pair.songA().getId()).isEqualTo(1);
        assertThat(pair.songB().getId()).isEqualTo(2);
    }

    @Test
    void swissTiesAreStableRegardlessOfRepositoryOrder() {
        List<Song> pool = new ArrayList<>(pool(6));
        Collections.reverse(pool);
        PairingPolicy.Pair pair = policy.select(pool, List.of(), List.of(), 5, 1500).orElseThrow();
        assertThat(pair.songA().getId()).isEqualTo(1);
        assertThat(pair.songB().getId()).isEqualTo(2);
    }

    @Test
    void unratedSongsUseConfiguredStartingRating() {
        List<Song> pool = pool(3);
        List<SongRating> ratings = List.of(rating(pool, 2, 1490), rating(pool, 3, 2490));
        assertThat(policy.select(pool, List.of(), ratings, 5, 2500).orElseThrow().songB().getId()).isEqualTo(3);
        assertThat(policy.select(pool, List.of(), ratings, 5, 1500).orElseThrow().songB().getId()).isEqualTo(2);
    }

    @Test
    void fullRunHasNoUnorderedRepeatsAndBalancedExposureForEqualRatings() {
        List<Song> pool = pool(25);
        List<Matchup> history = new ArrayList<>();
        Set<PairingPolicy.PairKey> keys = new HashSet<>();
        Map<Long, Integer> exposure = new HashMap<>();
        pool.forEach(song -> exposure.put(song.getId(), 0));
        for (int round = 1; round <= 16; round++) {
            PairingPolicy.Pair pair = policy.select(pool, history, List.of(), round, 1500, new Random(round)).orElseThrow();
            assertThat(keys.add(PairingPolicy.PairKey.of(pair.songA(), pair.songB()))).isTrue();
            exposure.merge(pair.songA().getId(), 1, Integer::sum);
            exposure.merge(pair.songB().getId(), 1, Integer::sum);
            // Store reversed order to prove canonical pair exclusion.
            history.add(new Matchup(run, pair.songB(), pair.songA(), pair.songA()));
        }
        assertThat(keys).hasSize(16);
        assertThat(Collections.max(exposure.values()) - Collections.min(exposure.values())).isLessThanOrEqualTo(1);
    }

    @Test
    void smallPoolExhaustsExactlyAllDistinctPairs() {
        List<Song> pool = pool(4);
        List<Matchup> history = new ArrayList<>();
        Set<PairingPolicy.PairKey> keys = new HashSet<>();
        for (int round = 1; round <= 6; round++) {
            assertThat(policy.isExhausted(pool, history)).isFalse();
            PairingPolicy.Pair pair = policy.select(pool, history, List.of(), round, 1500).orElseThrow();
            assertThat(keys.add(PairingPolicy.PairKey.of(pair.songA(), pair.songB()))).isTrue();
            history.add(new Matchup(run, pair.songA(), pair.songB(), pair.songA()));
        }
        assertThat(policy.isExhausted(pool, history)).isTrue();
        assertThat(policy.select(pool, history, List.of(), 7, 1500)).isEmpty();
        assertThat(policy.select(pool(1), List.of(), List.of(), 1, 1500)).isEmpty();
    }

    private List<Song> pool(int count) {
        return LongStream.rangeClosed(1, count).mapToObj(id -> {
            Song song = new Song("id-" + id, "Song " + id, "Artist", null, null, "rnb", 50, 2000);
            ReflectionTestUtils.setField(song, "id", id);
            return song;
        }).toList();
    }

    private Matchup vote(List<Song> pool, int a, int b) {
        return new Matchup(run, pool.get(a - 1), pool.get(b - 1), pool.get(a - 1));
    }

    private SongRating rating(List<Song> pool, int id, double value) {
        return new SongRating(run, pool.get(id - 1), value);
    }
}
