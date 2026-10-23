package com.intunes.service;

import com.intunes.domain.*;
import com.intunes.dto.MatchupPair;
import com.intunes.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static com.intunes.service.MatchupException.Reason.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false", "debug=false",
        "intunes.spotify.client-id=", "intunes.spotify.client-secret=", "intunes.lastfm.api-key=",
        "intunes.matchup.elo-start-rating=1800"
})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MatchupServiceTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MatchupService service;
    @Autowired PairingPolicy policy;
    @Autowired EloRatingService elo;
    @Autowired UserRepository users;
    @Autowired MatchupRunRepository runs;
    @Autowired SongRepository songs;
    @Autowired MatchupRepository votes;
    @Autowired SongRatingRepository ratings;
    @Autowired IssuedMatchupPairRepository issued;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    private User owner;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("truncate table users, songs, matchup_runs, song_ratings, matchups, taste_profiles, communities, posts, replies, likes, issued_matchup_pairs restart identity cascade");
        owner = users.saveAndFlush(new User("listener", "encoded", "Listener"));
    }

    @Test
    void firstPairIsDistinctPreviewlessAndStableAcrossRefreshAndServiceInstances() {
        pool("rnb", 4);
        assertThat(service.isComplete(owner, "rnb")).isFalse();
        assertThat(runs.count()).isZero();
        MatchupPair first = service.nextPair(owner, "rnb");
        assertThat(first.round()).isEqualTo(1);
        assertThat(first.totalRounds()).isEqualTo(16);
        assertThat(first.songA().id()).isNotEqualTo(first.songB().id());
        assertThat(first.songA().previewUrl()).isNull();
        assertThat(first.songB().previewUrl()).isNull();
        assertThat(service.nextPair(owner, "rnb")).isEqualTo(first);
        assertThat(service.nextPair(owner, first.runId())).isEqualTo(first);
        // A new service object, using a fresh transaction, must read the persisted pair.
        MatchupService restarted = new MatchupService(users, runs, songs, votes, ratings, issued, policy, 99, 9999);
        MatchupPair resumed = tx().execute(status -> restarted.nextPair(owner, first.runId()));
        assertThat(resumed).isEqualTo(first);
        assertThat(issued.count()).isEqualTo(1);
        assertThat(runs.count()).isEqualTo(1);
        assertThat(votes.count()).isZero();
        assertThat(ratings.count()).isZero();
    }

    @Test
    void sixteenRoundRunNeverRepeatsAndCompletionCommitsWhenNextPairSignalsTheEnd() {
        pool("rnb", 25);
        Set<String> pairs = new HashSet<>();
        MatchupPair pair = service.nextPair(owner, "rnb");
        Long runId = pair.runId();
        for (int round = 1; round <= 16; round++) {
            assertThat(pair.round()).isEqualTo(round);
            assertThat(pair.totalRounds()).isEqualTo(16);
            assertThat(pairs.add(key(pair))).isTrue();
            recordVote(pair, true);
            if (round < 16) pair = service.nextPair(owner, runId);
        }
        assertThatThrownBy(() -> service.nextPair(owner, runId)).isInstanceOf(RunCompleteException.class);
        assertThat(runs.findById(runId).orElseThrow().getStatus()).isEqualTo(MatchupRun.Status.COMPLETED);
        assertThat(service.isComplete(owner, "rnb")).isTrue();
        assertThat(service.isComplete(owner, runId)).isTrue();
        assertThatThrownBy(() -> service.nextPair(owner, "rnb")).isInstanceOf(RunCompleteException.class);
        assertThat(runs.count()).isEqualTo(1);
        assertThat(issued.count()).isEqualTo(16);
        assertThat(votes.count()).isEqualTo(16);
        assertThat(pairs).hasSize(16);
    }

    @Test
    void smallPoolExhaustsWithoutRepeatsAndIsCompleteUsesTheSameRule() {
        pool("rnb", 3);
        Set<String> pairs = new HashSet<>();
        MatchupPair pair = service.nextPair(owner, "rnb");
        Long runId = pair.runId();
        for (int round = 1; round <= 3; round++) {
            assertThat(service.isComplete(owner, runId)).isFalse();
            assertThat(pair.round()).isEqualTo(round);
            assertThat(pairs.add(key(pair))).isTrue();
            recordVote(pair, false); // Exercise reconciliation of already-persisted votes.
            if (round < 3) pair = service.nextPair(owner, runId);
        }
        assertThat(service.isComplete(owner, runId)).isTrue();
        assertThat(runs.findById(runId).orElseThrow().getStatus()).isEqualTo(MatchupRun.Status.COMPLETED);
        assertThatThrownBy(() -> service.nextPair(owner, runId)).isInstanceOf(RunCompleteException.class);
        assertThat(issued.findAll()).allSatisfy(persisted -> assertThat(persisted.getStatus()).isEqualTo(IssuedMatchupPair.Status.VOTED));
        assertThat(pairs).hasSize(3);
    }

    @Test
    void twoPreviewlessSongsStillSupportOneVoteAndARetakeCanRepeatThePair() {
        pool("gospel", 2);
        MatchupPair first = service.nextPair(owner, "gospel");
        recordVote(first, false);
        assertThat(service.isComplete(owner, "gospel")).isTrue();
        Long retake = service.startNewRun(owner, "gospel");
        MatchupPair second = service.nextPair(owner, retake);
        assertThat(key(second)).isEqualTo(key(first));
        assertThat(second.runId()).isNotEqualTo(first.runId());
        assertThat(second.issuedPairId()).isNotEqualTo(first.issuedPairId());
        assertThat(second.round()).isEqualTo(1);
        assertThat(service.isComplete(owner, retake)).isFalse();
        List<SongRating> freshRatings = tx().execute(status -> ratings.findByRunAndSongInOrderBySongIdAsc(
                runs.findById(retake).orElseThrow(), songs.findByGenre("gospel")));
        assertThat(freshRatings).isEmpty();
    }

    @Test
    void retakeAbandonsActiveRunAndPreservesItsVotesRatingsAndPendingPair() {
        pool("rnb", 6);
        MatchupPair first = service.nextPair(owner, "rnb");
        recordVote(first, true);
        MatchupPair pending = service.nextPair(owner, first.runId());
        Long fresh = service.startNewRun(owner, "rnb");
        assertThat(runs.findById(first.runId()).orElseThrow().getStatus()).isEqualTo(MatchupRun.Status.ABANDONED);
        assertReason(() -> service.nextPair(owner, first.runId()), RUN_ABANDONED);
        assertThat(service.nextPair(owner, fresh).round()).isEqualTo(1);
        assertThat(issued.findById(pending.issuedPairId())).isPresent();
        assertThat(votes.count()).isEqualTo(1);
        assertThat(ratings.count()).isEqualTo(2);
    }

    @Test
    void usersGenresAndExplicitRunIdsAreIsolated() {
        pool("rnb", 4);
        pool("country", 2);
        User other = users.saveAndFlush(new User("other", "encoded", null));
        MatchupPair first = service.nextPair(owner, "rnb");
        MatchupPair country = service.nextPair(owner, "country");
        MatchupPair otherPair = service.nextPair(other, "rnb");
        assertThat(Set.of(first.runId(), country.runId(), otherPair.runId())).hasSize(3);
        assertReason(() -> service.nextPair(other, first.runId()), RUN_NOT_FOUND);
        assertReason(() -> service.isComplete(other, first.runId()), RUN_NOT_FOUND);
        Optional<IssuedMatchupPair> forged = tx().execute(status -> issued.findByIdAndRunAndUser(first.issuedPairId(),
                runs.findById(first.runId()).orElseThrow(), other));
        assertThat(forged).isEmpty();
        assertThat(service.nextPair(owner, "rnb")).isEqualTo(first);
    }

    @Test
    void zeroAndOneSongPoolsFailCleanlyWithoutCreatingRuns() {
        assertReason(() -> service.nextPair(owner, "rnb"), INSUFFICIENT_SONGS);
        pool("rnb", 1);
        assertReason(() -> service.nextPair(owner, "rnb"), INSUFFICIENT_SONGS);
        assertReason(() -> service.isComplete(owner, "rnb"), INSUFFICIENT_SONGS);
        assertReason(() -> service.startNewRun(owner, "rnb"), INSUFFICIENT_SONGS);
        assertThat(runs.count()).isZero();
        assertThat(issued.count()).isZero();
    }

    @Test
    void invalidUsersAndGenresHaveDomainErrors() {
        assertReason(() -> service.nextPair(null, "rnb"), INVALID_USER);
        assertReason(() -> service.nextPair(new User("unsaved", "encoded", null), "rnb"), INVALID_USER);
        assertReason(() -> service.nextPair(owner, "latin"), INVALID_GENRE);
        assertReason(() -> service.nextPair(owner, (String) null), INVALID_GENRE);
        assertReason(() -> service.nextPair(owner, Long.MAX_VALUE), RUN_NOT_FOUND);
    }

    @Test
    void storedRoundLimitSurvivesConfigurationChanges() {
        pool("rnb", 5);
        Long id = tx().execute(status -> runs.saveAndFlush(new MatchupRun(owner, "rnb", 2)).getId());
        MatchupPair first = service.nextPair(owner, id);
        assertThat(first.totalRounds()).isEqualTo(2);
        recordVote(first, true);
        MatchupPair second = service.nextPair(owner, id);
        assertThat(second.round()).isEqualTo(2);
        assertThat(second.totalRounds()).isEqualTo(2);
        recordVote(second, true);
        assertThat(service.isComplete(owner, id)).isTrue();
    }

    @Test
    void concurrentFirstRequestsReturnOnePersistedPairAndOneRun() throws Exception {
        pool("rnb", 25);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<MatchupPair> request = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent start timed out");
                return service.nextPair(owner, "rnb");
            };
            var one = pool.submit(request);
            var two = pool.submit(request);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(one.get(10, TimeUnit.SECONDS)).isEqualTo(two.get(10, TimeUnit.SECONDS));
            assertThat(runs.count()).isEqualTo(1);
            assertThat(issued.count()).isEqualTo(1);
        } finally {
            start.countDown();
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void roundFiveUsesNearestRatingWithCustomStartingRatingForUnratedSongs() {
        pool("rnb", 6);
        Long id = service.startNewRun(owner, "rnb");
        tx().executeWithoutResult(status -> {
            MatchupRun run = runs.findById(id).orElseThrow();
            List<Song> s = songs.findByGenre("rnb").stream().sorted(Comparator.comparing(Song::getId)).toList();
            votes.save(new Matchup(run, s.get(0), s.get(2), s.get(0)));
            votes.save(new Matchup(run, s.get(1), s.get(3), s.get(1)));
            votes.save(new Matchup(run, s.get(4), s.get(5), s.get(4)));
            votes.save(new Matchup(run, s.get(2), s.get(3), s.get(2)));
            ratings.save(new SongRating(run, s.get(1), 1790));
            ratings.save(new SongRating(run, s.get(2), 1400));
            ratings.save(new SongRating(run, s.get(3), 1300));
            ratings.save(new SongRating(run, s.get(4), 1200));
            ratings.save(new SongRating(run, s.get(5), 1100));
        });
        MatchupPair pair = service.nextPair(owner, id);
        assertThat(pair.round()).isEqualTo(5);
        assertThat(pair.songA().id()).isEqualTo(1);
        assertThat(pair.songB().id()).isEqualTo(2);
    }

    /** Simulate the atomic persistence boundary owned by #24; the public vote endpoint remains a stub. */
    private void recordVote(MatchupPair pair, boolean resolveIssuedState) {
        tx().executeWithoutResult(status -> {
            MatchupRun run = runs.findByIdAndUserForUpdate(pair.runId(), owner).orElseThrow();
            Song a = songs.findById(pair.songA().id()).orElseThrow();
            Song b = songs.findById(pair.songB().id()).orElseThrow();
            Matchup vote = votes.saveAndFlush(new Matchup(run, a, b, a));
            SongRating ra = ratings.findByRunAndSong(run, a).orElseGet(() -> ratings.save(new SongRating(run, a, 1800)));
            SongRating rb = ratings.findByRunAndSong(run, b).orElseGet(() -> ratings.save(new SongRating(run, b, 1800)));
            double[] updated = elo.update(ra.getRating(), rb.getRating());
            ra.setRating(updated[0]);
            rb.setRating(updated[1]);
            ra.setMatchupsPlayed(ra.getMatchupsPlayed() + 1);
            rb.setMatchupsPlayed(rb.getMatchupsPlayed() + 1);
            if (resolveIssuedState) issued.findById(pair.issuedPairId()).orElseThrow().markVoted(vote);
        });
    }

    private void pool(String genre, int count) {
        for (int i = 0; i < count; i++) {
            songs.saveAndFlush(new Song(genre + i, "Song " + i, "Artist", null, null, genre, 50, 2000));
        }
    }

    private String key(MatchupPair pair) {
        return Math.min(pair.songA().id(), pair.songB().id()) + ":" + Math.max(pair.songA().id(), pair.songB().id());
    }

    private TransactionTemplate tx() { return new TransactionTemplate(transactions); }

    private void assertReason(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, MatchupException.Reason reason) {
        assertThatThrownBy(action).isInstanceOfSatisfying(MatchupException.class,
                exception -> assertThat(exception.getReason()).isEqualTo(reason));
    }
}
