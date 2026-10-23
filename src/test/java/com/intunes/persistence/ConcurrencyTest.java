package com.intunes.persistence;

import com.intunes.domain.*;
import com.intunes.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=validate", "debug=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired PlatformTransactionManager transactions;
    @Autowired UserRepository users;
    @Autowired MatchupRunRepository runs;
    @Autowired SongRatingRepository ratings;
    @Autowired CommunityRepository communities;
    @Autowired ReplyRepository replies;
    @Autowired LikeRepository likes;
    @PersistenceContext EntityManager em;

    @Test
    void runLockSerializesFirstRatingCreationAndPreventsLostEloUpdates() throws Exception {
        Fixture f = fixture();
        concurrently(() -> {
            MatchupRun run = runs.findByIdAndUserForUpdate(f.run().getId(), f.user()).orElseThrow();
            SongRating rating = ratings.findByRunAndSong(run, f.song()).orElseGet(() -> persist(new SongRating(run, f.song())));
            rating.setRating(rating.getRating() + 16);
            rating.setMatchupsPlayed(rating.getMatchupsPlayed() + 1);
            em.flush();
        });
        tx().executeWithoutResult(status -> {
            SongRating rating = ratings.findByRunAndSong(f.run(), f.song()).orElseThrow();
            assertThat(rating.getRating()).isEqualTo(1532);
            assertThat(rating.getMatchupsPlayed()).isEqualTo(2);
        });
    }

    @Test
    void userLockSerializesRetakesAndPreservesExactlyOneActiveRun() throws Exception {
        Fixture f = fixture();
        concurrently(() -> {
            users.findByIdForUpdate(f.user().getId()).orElseThrow();
            runs.findActive(f.user(), "rnb").orElseThrow().abandon();
            em.flush(); // Release the partial unique key before inserting the new attempt.
            persist(new MatchupRun(f.user(), "rnb"));
        });
        tx().executeWithoutResult(status -> {
            assertThat(runs.findActive(f.user(), "rnb")).isPresent();
            assertThat(em.createQuery("select count(r) from MatchupRun r where r.user=:user", Long.class)
                    .setParameter("user", f.user()).getSingleResult()).isEqualTo(3);
        });
    }

    @Test
    void userLockSerializesPinSwapsWithOnePinAtCommit() throws Exception {
        Fixture f = fixture();
        AtomicInteger turn = new AtomicInteger();
        concurrently(() -> {
            users.findByIdForUpdate(f.user().getId()).orElseThrow();
            replies.findByAuthorAndPinnedTrue(f.user()).ifPresent(reply -> reply.setPinned(false));
            em.flush(); // Unpin before pinning: the PostgreSQL partial unique index is immediate.
            Long target = turn.getAndIncrement() == 0 ? f.first().getId() : f.second().getId();
            em.find(Reply.class, target).setPinned(true);
            em.flush();
        });
        tx().executeWithoutResult(status -> assertThat(replies.findByAuthorAndPinnedTrue(f.user()))
                .get().extracting(Reply::getId).isEqualTo(f.second().getId()));
    }

    @Test
    void concurrentCommunityCreationAndLikesAreIdempotent() throws Exception {
        Fixture f = fixture();
        String key = "NEW_" + UUID.randomUUID();
        AtomicInteger created = new AtomicInteger();
        AtomicInteger liked = new AtomicInteger();
        concurrently(() -> {
            created.addAndGet(communities.insertIfAbsent(key, "Community", null));
            liked.addAndGet(likes.likePostIfAbsent(f.user().getId(), f.post().getId()));
        });
        assertThat(created).hasValue(1);
        assertThat(liked).hasValue(1);
        tx().executeWithoutResult(status -> {
            assertThat(communities.findByArchetypeKey(key)).isPresent();
            assertThat(likes.countByPost(f.post())).isEqualTo(1);
        });
    }

    @Test
    void optimisticVersionRejectsAStaleRatingEvenWhenCallerOmitsRunLock() {
        Fixture f = fixture();
        SongRating stale = tx().execute(status -> {
            SongRating rating = persist(new SongRating(f.run(), f.song()));
            em.flush();
            return rating;
        });
        tx().executeWithoutResult(status -> ratings.findById(stale.getId()).orElseThrow().setRating(1600));
        stale.setRating(1400);
        assertThatThrownBy(() -> tx().executeWithoutResult(status -> {
            em.merge(stale);
            em.flush();
        })).isInstanceOfAny(jakarta.persistence.OptimisticLockException.class,
                org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void voteAndRatingChangesRollbackTogether() {
        Fixture f = fixture();
        assertThatThrownBy(() -> tx().executeWithoutResult(status -> {
            MatchupRun run = runs.findByIdAndUserForUpdate(f.run().getId(), f.user()).orElseThrow();
            Song other = persist(new Song(UUID.randomUUID().toString(), "Other", "Artist", null, null, "rnb", 50, 1990));
            persist(new SongRating(run, f.song(), 1516));
            persist(new Matchup(run, f.song(), other, f.song()));
            em.flush();
            throw new IllegalStateException("simulate failure before commit");
        })).isInstanceOf(IllegalStateException.class);
        tx().executeWithoutResult(status -> {
            assertThat(ratings.findByRunAndSong(f.run(), f.song())).isEmpty();
            assertThat(em.createQuery("select count(m) from Matchup m where m.run=:run", Long.class)
                    .setParameter("run", f.run()).getSingleResult()).isZero();
        });
    }

    private TransactionTemplate tx() {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.setTimeout(10);
        return transaction;
    }

    private void concurrently(Runnable operation) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Runnable worker = () -> tx().executeWithoutResult(status -> {
                ready.countDown();
                await(start);
                operation.run();
            });
            var first = pool.submit(worker);
            var second = pool.submit(worker);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test start timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private Fixture fixture() {
        return tx().execute(status -> {
            String key = UUID.randomUUID().toString();
            User user = persist(new User(key, "encoded", "Listener"));
            Song song = persist(new Song(key, "Song", "Artist", null, null, "rnb", 50, 1990));
            MatchupRun run = persist(new MatchupRun(user, "rnb"));
            Community community = persist(new Community(key, "Community", null));
            Post post = persist(new Post(community, user, "Post"));
            Reply first = persist(new Reply(post, user, "First"));
            Reply second = persist(new Reply(post, user, "Second"));
            return new Fixture(user, song, run, post, first, second);
        });
    }

    private record Fixture(User user, Song song, MatchupRun run, Post post, Reply first, Reply second) { }
}
