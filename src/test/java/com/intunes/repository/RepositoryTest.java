package com.intunes.repository;

import com.intunes.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=validate", "debug=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RepositoryTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired TestEntityManager entities;
    @Autowired UserRepository users;
    @Autowired MatchupRunRepository runs;
    @Autowired MatchupRepository votes;
    @Autowired SongRatingRepository ratings;
    @Autowired TasteProfileRepository profiles;
    @Autowired CommunityRepository communities;
    @Autowired PostRepository posts;
    @Autowired ReplyRepository replies;
    @Autowired LikeRepository likes;
    @Autowired SongRepository songs;

    @Test
    void identityAndProfileLookupsRespectOwnersGenresAndMissingRows() {
        Fixture f = fixture();
        assertThat(users.findByUsername("alice")).contains(f.alice());
        assertThat(users.findByUsername("missing")).isEmpty();
        assertThat(profiles.findByUserAndGenre(f.alice(), "rnb")).contains(f.profile());
        assertThat(profiles.findByUserAndGenre(f.bob(), "rnb")).isEmpty();
        assertThat(profiles.findByUserAndGenre(f.alice(), "country")).isEmpty();
        assertThat(communities.findByArchetypeKey("RNB_DEEP_90S")).contains(f.community());
        assertThat(communities.findByArchetypeKey("missing")).isEmpty();
        assertThat(runs.findActive(f.alice(), "rnb")).contains(f.active());
        assertThat(runs.findActive(f.bob(), "country")).isEmpty();
        assertThat(runs.findLatestCompleted(f.alice(), "rnb")).contains(f.latest());
        assertThat(runs.findByIdAndUserForUpdate(f.active().getId(), f.bob())).isEmpty();
        assertThat(runs.findByIdAndUserForUpdate(f.active().getId(), f.alice())).contains(f.active());
    }

    @Test
    void voteHistoryAndRoundCountsKeepAttemptsSeparateAndRetakesMayRepeatPairs() {
        Fixture f = fixture();
        Matchup first = entities.persist(new Matchup(f.old(), f.a(), f.b(), f.a()));
        Matchup retake = entities.persist(new Matchup(f.latest(), f.b(), f.a(), f.b()));
        entities.persist(new Matchup(f.active(), f.a(), f.b(), f.a()));
        MatchupRun other = completedRun(f.bob(), "rnb");
        entities.persist(new Matchup(other, f.a(), f.b(), f.b()));
        entities.flush();
        assertThat(votes.findByRunOrderByCreatedAtAscIdAsc(f.old())).containsExactly(first);
        assertThat(votes.findByRunOrderByCreatedAtAscIdAsc(f.latest())).containsExactly(retake);
        assertThat(votes.countByRun(f.active())).isEqualTo(1);
        assertThat(votes.countByUserAndGenre(f.alice(), "rnb")).isEqualTo(3);
        assertThat(votes.findByUserAndGenre(f.alice(), "rnb")).hasSize(3)
                .allSatisfy(vote -> assertThat(vote.getUser()).isEqualTo(f.alice()));
        assertThat(votes.findByUserAndGenre(f.alice(), "country")).isEmpty();
    }

    @Test
    void profileRatingReadsUseOnlyLatestCompletedRunAndHaveStableRanking() {
        Fixture f = fixture();
        entities.persist(new SongRating(f.old(), f.a(), 9999));
        SongRating a = entities.persist(new SongRating(f.latest(), f.a(), 1700));
        SongRating b = entities.persist(new SongRating(f.latest(), f.b(), 1700));
        SongRating current = entities.persist(new SongRating(f.active(), f.a(), 1500));
        MatchupRun abandoned = new MatchupRun(f.alice(), "rnb");
        abandoned.abandon();
        entities.persist(abandoned);
        entities.persist(new SongRating(abandoned, f.b(), 9999));
        MatchupRun country = completedRun(f.alice(), "country");
        entities.persist(new SongRating(country, f.c(), 9999));
        MatchupRun bobRun = completedRun(f.bob(), "rnb");
        entities.persist(new SongRating(bobRun, f.a(), 9999));
        entities.flush();
        assertThat(ratings.findByUserAndSong(f.alice(), f.a())).contains(a);
        assertThat(ratings.findByUserAndSongIn(f.alice(), List.of(f.a(), f.b()))).containsExactly(a, b);
        assertThat(ratings.findByUserAndSongIn(f.alice(), List.of())).isEmpty();
        assertThat(ratings.findByRunAndSong(f.active(), f.a())).contains(current);
        assertThat(ratings.findByRunAndSongInOrderBySongIdAsc(f.latest(), List.of(f.b(), f.a()))).containsExactly(a, b);
        assertThat(ratings.findTopRatedByUserAndGenre(f.alice(), "rnb", PageRequest.of(0, 1))).containsExactly(a);
        assertThat(ratings.findTopRatedByUserAndGenre(f.alice(), "rnb", PageRequest.of(1, 1))).containsExactly(b);
        assertThat(ratings.findByRunOrderByRatingDescSongIdAsc(f.latest(), PageRequest.of(0, 5))).containsExactly(a, b);
        assertThat(songs.findByGenre("rnb")).contains(f.a(), f.b());
        assertThat(f.a().getPreviewUrl()).isNull();
    }

    @Test
    void latestCompletedTieBreaksOnRunIdAndNeverFallsBackToAnOlderSongsRating() {
        Fixture f = fixture();
        entities.persist(new SongRating(f.old(), f.a(), 1600));
        entities.persist(new SongRating(f.latest(), f.b(), 1700));
        entities.flush();
        entities.getEntityManager().createNativeQuery("update matchup_runs set finished_at = :at where status='COMPLETED'")
                .setParameter("at", Instant.now().plusSeconds(1)).executeUpdate();
        entities.clear();
        assertThat(runs.findLatestCompleted(f.alice(), "rnb")).get()
                .extracting(MatchupRun::getId).isEqualTo(f.latest().getId());
        assertThat(ratings.findByUserAndSong(f.alice(), f.a())).isEmpty();
        assertThat(ratings.findTopRatedByUserAndGenre(f.alice(), "rnb", PageRequest.of(0, 5)))
                .extracting(rating -> rating.getSong().getId()).containsExactly(f.b().getId());
    }

    @Test
    void boardPaginationRepliesPinsAndLikesAreScopedAndDeterministicallyOrdered() {
        Fixture f = fixture();
        Post first = entities.persist(new Post(f.community(), f.alice(), "First"));
        Post second = entities.persist(new Post(f.community(), f.bob(), "Second"));
        Community elsewhere = entities.persist(new Community("COUNTRY_DEEP_90S", "Elsewhere", null));
        entities.persist(new Post(elsewhere, f.alice(), "Other community"));
        Reply one = entities.persist(new Reply(first, f.alice(), "One"));
        Reply two = entities.persist(new Reply(first, f.bob(), "Two"));
        Reply elsewhereReply = entities.persist(new Reply(second, f.alice(), "Other post"));
        one.setPinned(true);
        entities.flush();
        entities.getEntityManager().createNativeQuery("update posts set created_at = current_timestamp").executeUpdate();
        entities.getEntityManager().createNativeQuery("update replies set created_at = current_timestamp").executeUpdate();
        assertThat(posts.findByCommunityOrderByCreatedAtDescIdDesc(f.community(), PageRequest.of(0, 1)).getContent()).containsExactly(second);
        assertThat(posts.findByCommunityOrderByCreatedAtDescIdDesc(f.community(), PageRequest.of(1, 1)).getContent()).containsExactly(first);
        assertThat(posts.findByCommunityOrderByCreatedAtDescIdDesc(f.community(), PageRequest.of(2, 1))).isEmpty();
        assertThat(replies.findByPost(first)).containsExactly(one, two).doesNotContain(elsewhereReply);
        assertThat(replies.findPinnedReplies(f.alice())).singleElement().satisfies(pin -> {
            assertThat(pin.id()).isEqualTo(one.getId());
            assertThat(pin.body()).isEqualTo("One");
            assertThat(pin.from()).isEqualTo("Reply · Community");
        });
        assertThat(replies.findPinnedReplies(f.bob())).isEmpty();
        assertThat(likes.likePostIfAbsent(f.alice().getId(), first.getId())).isEqualTo(1);
        assertThat(likes.likePostIfAbsent(f.alice().getId(), first.getId())).isZero();
        assertThat(likes.likeReplyIfAbsent(f.bob().getId(), one.getId())).isEqualTo(1);
        assertThat(likes.likeReplyIfAbsent(f.bob().getId(), one.getId())).isZero();
        assertThat(likes.existsByUserAndPost(f.alice(), first)).isTrue();
        assertThat(likes.existsByUserAndPost(f.bob(), first)).isFalse();
        assertThat(likes.countByPost(first)).isEqualTo(1);
        assertThat(likes.countByPost(second)).isZero();
        assertThat(likes.existsByUserAndReply(f.bob(), one)).isTrue();
        assertThat(likes.countByReply(one)).isEqualTo(1);
    }

    @Test
    void communityInsertIsIdempotentAndProfileCanBeReplacedInPlace() {
        Fixture f = fixture();
        assertThat(communities.insertIfAbsent("RNB_DEEP_90S", "Duplicate", "Ignored")).isZero();
        assertThat(communities.insertIfAbsent("GOSPEL_DEEP_90S", "New community", null)).isEqualTo(1);
        assertThat(communities.findByArchetypeKey("GOSPEL_DEEP_90S")).isPresent();
        f.profile().updateFromRun(f.latest(), "RNB_MAINSTREAM_NOW", "New archetype");
        entities.flush();
        entities.clear();
        TasteProfile profile = profiles.findByUserAndGenre(f.alice(), "rnb").orElseThrow();
        assertThat(profile.getId()).isEqualTo(f.profile().getId());
        assertThat(profile.getRun().getId()).isEqualTo(f.latest().getId());
        assertThat(profile.getArchetypeKey()).isEqualTo("RNB_MAINSTREAM_NOW");
        assertThat(profiles.count()).isEqualTo(1);
    }

    private MatchupRun completedRun(User user, String genre) {
        MatchupRun run = entities.persist(new MatchupRun(user, genre));
        run.complete();
        entities.flush();
        return run;
    }

    private Fixture fixture() {
        User alice = entities.persist(new User("alice", "encoded", "Alice"));
        User bob = entities.persist(new User("bob", "encoded", "Bob"));
        Song a = entities.persist(new Song("a", "A", "Artist", null, null, "rnb", 50, 1990));
        Song b = entities.persist(new Song("b", "B", "Artist", null, null, "rnb", 80, 2020));
        Song c = entities.persist(new Song("c", "C", "Artist", null, null, "country", 80, 2020));
        MatchupRun old = completedRun(alice, "rnb");
        MatchupRun latest = completedRun(alice, "rnb");
        MatchupRun active = entities.persist(new MatchupRun(alice, "rnb"));
        TasteProfile profile = entities.persist(new TasteProfile(old, "RNB_DEEP_90S", "Test archetype"));
        Community community = entities.persist(new Community("RNB_DEEP_90S", "Community", null));
        return new Fixture(alice, bob, a, b, c, old, latest, active, profile, community);
    }

    private record Fixture(User alice, User bob, Song a, Song b, Song c, MatchupRun old,
                           MatchupRun latest, MatchupRun active, TasteProfile profile, Community community) { }
}
