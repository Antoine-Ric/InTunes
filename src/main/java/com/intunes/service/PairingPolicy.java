package com.intunes.service;

import com.intunes.domain.Matchup;
import com.intunes.domain.Song;
import com.intunes.domain.SongRating;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/** Bounded selection over the genre pool. No retry-until-lucky random loops. */
@Component
public class PairingPolicy {
    public record Pair(Song songA, Song songB) { }

    public record PairKey(long low, long high) {
        public static PairKey of(Song a, Song b) {
            return new PairKey(Math.min(a.getId(), b.getId()), Math.max(a.getId(), b.getId()));
        }
    }

    public Optional<Pair> select(List<Song> pool, List<Matchup> votes, List<SongRating> ratings,
                                 int round, double startingRating) {
        return select(pool, votes, ratings, round, startingRating, ThreadLocalRandom.current());
    }

    Optional<Pair> select(List<Song> pool, List<Matchup> votes, List<SongRating> ratings,
                          int round, double startingRating, RandomGenerator random) {
        List<Pair> available = availablePairs(pool, votes);
        if (available.isEmpty()) return Optional.empty();
        Map<Long, Integer> exposure = new HashMap<>();
        votes.forEach(vote -> {
            exposure.merge(vote.getSongA().getId(), 1, Integer::sum);
            exposure.merge(vote.getSongB().getId(), 1, Integer::sum);
        });
        Comparator<Song> byExposure = Comparator.comparingInt(song -> exposure.getOrDefault(song.getId(), 0));
        List<Song> anchors = available.stream().flatMap(pair -> java.util.stream.Stream.of(pair.songA(), pair.songB()))
                .distinct().sorted(byExposure.thenComparing(Song::getId)).toList();
        int leastExposure = exposure.getOrDefault(anchors.getFirst().getId(), 0);
        List<Song> leastExposed = anchors.stream()
                .filter(song -> exposure.getOrDefault(song.getId(), 0) == leastExposure).toList();
        Song anchor = round <= 4 ? leastExposed.get(random.nextInt(leastExposed.size())) : leastExposed.getFirst();
        List<Song> opponents = available.stream()
                .filter(pair -> pair.songA().getId().equals(anchor.getId()) || pair.songB().getId().equals(anchor.getId()))
                .map(pair -> pair.songA().getId().equals(anchor.getId()) ? pair.songB() : pair.songA()).toList();
        Song opponent;
        if (round <= 4) {
            int minimum = opponents.stream().mapToInt(song -> exposure.getOrDefault(song.getId(), 0)).min().orElseThrow();
            List<Song> choices = opponents.stream().filter(song -> exposure.getOrDefault(song.getId(), 0) == minimum).toList();
            opponent = choices.get(random.nextInt(choices.size()));
        } else {
            Map<Long, Double> elo = new HashMap<>();
            ratings.forEach(rating -> elo.put(rating.getSong().getId(), rating.getRating()));
            double anchorRating = elo.getOrDefault(anchor.getId(), startingRating);
            opponent = opponents.stream().min(Comparator
                    .comparingDouble((Song song) -> Math.abs(elo.getOrDefault(song.getId(), startingRating) - anchorRating))
                    .thenComparing(byExposure).thenComparing(Song::getId)).orElseThrow();
        }
        return Optional.of(new Pair(anchor, opponent));
    }

    public boolean isExhausted(List<Song> pool, List<Matchup> votes) {
        return availablePairs(pool, votes).isEmpty();
    }

    private List<Pair> availablePairs(List<Song> pool, List<Matchup> votes) {
        Set<PairKey> used = new HashSet<>();
        votes.forEach(vote -> used.add(PairKey.of(vote.getSongA(), vote.getSongB())));
        List<Song> ordered = pool.stream().sorted(Comparator.comparing(Song::getId)).toList();
        List<Pair> available = new ArrayList<>();
        for (int a = 0; a < ordered.size(); a++) {
            for (int b = a + 1; b < ordered.size(); b++) {
                if (!used.contains(PairKey.of(ordered.get(a), ordered.get(b)))) {
                    available.add(new Pair(ordered.get(a), ordered.get(b)));
                }
            }
        }
        return available;
    }
}
