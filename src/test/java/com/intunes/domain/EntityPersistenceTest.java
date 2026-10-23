package com.intunes.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "debug=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
// Close the datasource before Testcontainers removes PostgreSQL.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EntityPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entities;

    @Test
    void constructorsProducePersistableEntitiesThatJpaCanReload() {
        Instant before = Instant.now().minusSeconds(1);
        Fixture fixture = persistFixture();
        entities.flush();
        entities.clear();

        User user = entities.find(User.class, fixture.user().getId());
        assertThat(user.getUsername()).isEqualTo("listener");
        assertThat(user.getDisplayName()).isEqualTo("Listener");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));

        SongRating rating = entities.find(SongRating.class, fixture.rating().getId());
        assertThat(rating.getUser()).isSameAs(user);
        assertThat(rating.getSong().getId()).isEqualTo(fixture.songA().getId());
        assertThat(rating.getRating()).isEqualTo(1500.0);
        assertThat(rating.getMatchupsPlayed()).isZero();

        Matchup vote = entities.find(Matchup.class, fixture.vote().getId());
        assertThat(vote.getUser()).isSameAs(user);
        assertThat(vote.getSongA()).isSameAs(rating.getSong());
        assertThat(vote.getSongB().getId()).isEqualTo(fixture.songB().getId());
        assertThat(vote.getWinner()).isSameAs(vote.getSongB());
        assertThat(vote.getGenre()).isEqualTo("gospel");
        assertThat(vote.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));

        TasteProfile profile = entities.find(TasteProfile.class, fixture.profile().getId());
        assertThat(profile.getUser()).isSameAs(user);
        assertThat(profile.getGenre()).isEqualTo("gospel");
        assertThat(profile.getArchetypeKey()).isEqualTo("GOSPEL_DEEP_90S");
        assertThat(profile.getArchetypeName()).isEqualTo("Test archetype");
        assertThat(profile.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));

        Community community = entities.find(Community.class, fixture.community().getId());
        assertThat(community.getArchetypeKey()).isEqualTo(profile.getArchetypeKey());
        assertThat(community.getName()).isEqualTo("Test community");
        assertThat(community.getDescription()).isNull();

        Post post = entities.find(Post.class, fixture.post().getId());
        assertThat(post.getCommunity()).isSameAs(community);
        assertThat(post.getAuthor()).isSameAs(user);
        assertThat(post.getBody()).isEqualTo("A song worth sharing");
        assertThat(post.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));

        Reply reply = entities.find(Reply.class, fixture.reply().getId());
        assertThat(reply.getPost()).isSameAs(post);
        assertThat(reply.getAuthor()).isSameAs(user);
        assertThat(reply.getBody()).isEqualTo("Agreed");
        assertThat(reply.isPinned()).isFalse();
        assertThat(reply.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));

        Like postLike = entities.find(Like.class, fixture.postLike().getId());
        assertThat(postLike.getUser()).isSameAs(user);
        assertThat(postLike.getPost()).isSameAs(post);
        assertThat(postLike.getReply()).isNull();
        assertThat(postLike.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));

        Like replyLike = entities.find(Like.class, fixture.replyLike().getId());
        assertThat(replyLike.getUser()).isSameAs(user);
        assertThat(replyLike.getPost()).isNull();
        assertThat(replyLike.getReply()).isSameAs(reply);
        assertThat(replyLike.getCreatedAt()).isBetween(before, Instant.now().plusSeconds(1));
    }

    @Test
    void editsArePersistedWithoutReplacingIdentityOrCreationTime() {
        Fixture fixture = persistFixture();
        SongRating configuredRating = entities.persist(new SongRating(fixture.rating().getRun(), fixture.songB(), 1600.0));
        entities.flush();
        entities.clear();

        User user = entities.find(User.class, fixture.user().getId());
        Instant userCreatedAt = user.getCreatedAt();
        user.setDisplayName("New display name");
        user.setPasswordHash("replacement-encoded-password");

        SongRating rating = entities.find(SongRating.class, configuredRating.getId());
        assertThat(rating.getRating()).isEqualTo(1600.0);
        rating.setRating(1616.0);
        rating.setMatchupsPlayed(1);

        TasteProfile profile = entities.find(TasteProfile.class, fixture.profile().getId());
        Instant profileCreatedAt = profile.getCreatedAt();
        profile.setArchetypeKey("GOSPEL_MAINSTREAM_NOW");
        profile.setArchetypeName("Updated test archetype");

        Community community = entities.find(Community.class, fixture.community().getId());
        community.setName("Updated community");
        community.setDescription("A description");

        Post post = entities.find(Post.class, fixture.post().getId());
        Instant postCreatedAt = post.getCreatedAt();
        post.setBody("Edited post");

        Reply reply = entities.find(Reply.class, fixture.reply().getId());
        Instant replyCreatedAt = reply.getCreatedAt();
        reply.setBody("Edited reply");
        reply.setPinned(true);
        entities.flush();
        entities.clear();

        user = entities.find(User.class, fixture.user().getId());
        assertThat(user.getDisplayName()).isEqualTo("New display name");
        assertThat(user.getPasswordHash()).isEqualTo("replacement-encoded-password");
        assertThat(user.getCreatedAt()).isEqualTo(userCreatedAt);

        rating = entities.find(SongRating.class, configuredRating.getId());
        assertThat(rating.getRating()).isEqualTo(1616.0);
        assertThat(rating.getMatchupsPlayed()).isEqualTo(1);

        profile = entities.find(TasteProfile.class, fixture.profile().getId());
        assertThat(profile.getArchetypeKey()).isEqualTo("GOSPEL_MAINSTREAM_NOW");
        assertThat(profile.getArchetypeName()).isEqualTo("Updated test archetype");
        assertThat(profile.getCreatedAt()).isEqualTo(profileCreatedAt);
        assertThat(entities.getEntityManager().createQuery("select count(p) from TasteProfile p", Long.class)
                .getSingleResult()).isEqualTo(1L);

        community = entities.find(Community.class, fixture.community().getId());
        assertThat(community.getName()).isEqualTo("Updated community");
        assertThat(community.getDescription()).isEqualTo("A description");

        post = entities.find(Post.class, fixture.post().getId());
        assertThat(post.getBody()).isEqualTo("Edited post");
        assertThat(post.getCreatedAt()).isEqualTo(postCreatedAt);

        reply = entities.find(Reply.class, fixture.reply().getId());
        assertThat(reply.getBody()).isEqualTo("Edited reply");
        assertThat(reply.isPinned()).isTrue();
        assertThat(reply.getCreatedAt()).isEqualTo(replyCreatedAt);
    }

    private Fixture persistFixture() {
        User user = entities.persist(new User("listener", "encoded-password", "Listener"));
        Song songA = entities.persist(new Song("track-a", "Song A", "Artist A", null, null, "gospel", 50, 1995));
        Song songB = entities.persist(new Song("track-b", "Song B", "Artist B", null, null, "gospel", 80, 2020));
        MatchupRun run = entities.persist(new MatchupRun(user, "gospel"));
        SongRating rating = entities.persist(new SongRating(run, songA));
        Matchup vote = entities.persist(new Matchup(run, songA, songB, songB));
        run.complete();
        TasteProfile profile = entities.persist(new TasteProfile(run, "GOSPEL_DEEP_90S", "Test archetype"));
        Community community = entities.persist(new Community("GOSPEL_DEEP_90S", "Test community", null));
        Post post = entities.persist(new Post(community, user, "A song worth sharing"));
        Reply reply = entities.persist(new Reply(post, user, "Agreed"));
        Like postLike = entities.persist(new Like(user, post));
        Like replyLike = entities.persist(new Like(user, reply));
        return new Fixture(user, songA, songB, rating, vote, profile, community, post, reply, postLike, replyLike);
    }

    private record Fixture(User user, Song songA, Song songB, SongRating rating, Matchup vote,
                           TasteProfile profile, Community community, Post post, Reply reply,
                           Like postLike, Like replyLike) {
    }
}
