package com.intunes.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class MigrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private String schema;

    @BeforeEach
    void isolatedSchema() {
        schema = "test_" + UUID.randomUUID().toString().replace("-", "");
        new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()))
                .execute("create schema " + schema);
        String url = postgres.getJdbcUrl() + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
        dataSource = new DriverManagerDataSource(url, postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration configuration() {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema);
    }

    @Test
    void freshDatabaseMigratesAndSecondStartupDoesNothing() {
        Flyway flyway = configuration().load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(jdbc.queryForObject("select count(*) from matchup_runs", Long.class)).isZero();
    }

    @Test
    void explicitBaselinePreservesLegacyDataAndLinksHistoricalRuns() {
        legacyDatabase();
        // Exercise the arbitrary Hibernate constraint name used by existing local databases.
        jdbc.execute("alter table song_ratings rename constraint uq_ratings_user_song to uk_legacy_random_name");
        jdbc.execute("drop table flyway_schema_history");
        Flyway flyway = configuration().baselineOnMigrate(true).baselineVersion("1").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where type = 'BASELINE'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from songs", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from songs where preview_url is null", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select rating from song_ratings where id = 1", Double.class)).isEqualTo(1532.0);
        assertThat(jdbc.queryForObject("select status from matchup_runs where user_id = 1", String.class)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select status from matchup_runs where user_id = 2", String.class)).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject("""
                select count(*) from taste_profiles p join matchups m on m.run_id = p.run_id
                join song_ratings r on r.run_id = p.run_id where p.id = 1
                """, Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select body from replies where id = 1", String.class)).isEqualTo("Legacy reply");
        assertThat(jdbc.queryForObject("select count(*) from likes", Long.class)).isEqualTo(1);
        // The old global user/song unique key has been replaced; a retake starts fresh.
        jdbc.update("insert into matchup_runs(user_id,genre,status,started_at) values (1,'rnb','IN_PROGRESS',now())");
        jdbc.update("""
                insert into song_ratings(user_id,song_id,genre,run_id,rating,matchups_played)
                select 1,1,'rnb',id,1500,0 from matchup_runs where user_id=1 and status='IN_PROGRESS'
                """);
        assertThat(jdbc.queryForObject("select count(*) from song_ratings where user_id=1 and song_id=1", Long.class)).isEqualTo(2);
    }

    @Test
    void unversionedNonemptyDatabaseRequiresExplicitBaseline() {
        legacyDatabase();
        jdbc.execute("drop table flyway_schema_history");
        assertThatThrownBy(() -> configuration().load().migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty");
    }

    @Test
    void duplicateLegacyPairsStopUpgradeWithoutDeletingHistoryOrPartiallyAlteringTables() {
        legacyDatabase();
        jdbc.update("""
                insert into matchups(user_id,songa_id,songb_id,winner_id,genre,created_at)
                values (1,2,1,1,'rnb',now())
                """);
        assertThatThrownBy(() -> configuration().load().migrate()).isInstanceOf(FlywayException.class)
                .hasMessageContaining("uq_matchups_run_pair");
        assertThat(jdbc.queryForObject("select count(*) from matchups", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                select count(*) from information_schema.columns where table_schema=?
                and table_name='song_ratings' and column_name='run_id'
                """, Long.class, schema)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "insert into likes(user_id,created_at) values (1,now())",
            "insert into likes(user_id,post_id,reply_id,created_at) values (1,1,1,now())",
            "insert into likes(user_id,post_id,created_at) values (1,1,now())",
            "insert into likes(user_id,reply_id,created_at) values (1,1,now()),(1,1,now())",
            "update replies set pinned=true; insert into replies(post_id,author_id,body,pinned,created_at) values (1,1,'second pin',true,now())",
            "insert into matchup_runs(user_id,genre,status,started_at) values (2,'rock','IN_PROGRESS',now())",
            "insert into matchup_runs(user_id,genre,status,started_at) values (1,'latin','IN_PROGRESS',now())",
            "insert into matchup_runs(user_id,genre,status,started_at) values (1,'pop','COMPLETED',now())",
            "insert into matchups(user_id,songa_id,songb_id,winner_id,genre,created_at,run_id) select 1,2,1,1,'rnb',now(),id from matchup_runs where user_id=1",
            "insert into matchups(user_id,songa_id,songb_id,winner_id,genre,created_at,run_id) select 1,1,1,1,'rnb',now(),id from matchup_runs where user_id=1",
            "update matchups set winner_id=3",
            "update matchups set songb_id=3",
            "update matchups set user_id=2",
            "update song_ratings set user_id=2 where id=1",
            "update song_ratings set song_id=3 where id=1",
            "update song_ratings set matchups_played=-1",
            "insert into song_ratings(user_id,song_id,genre,run_id,rating,matchups_played) select user_id,song_id,genre,run_id,1500,0 from song_ratings where id=1",
            "insert into taste_profiles(user_id,genre,archetype_key,archetype_name,created_at,run_id) select user_id,genre,archetype_key,archetype_name,now(),run_id from taste_profiles",
            "insert into users(username,password_hash,created_at) values ('alice','hash',now())",
            "insert into communities(archetype_key,name) values ('RNB_DEEP_90S','duplicate')",
            "insert into songs(spotify_id,title,artist,genre) values ('spotify-a','duplicate','artist','rnb')"
    })
    void databaseRejectsInvalidRows(String invalidSql) {
        legacyDatabase();
        configuration().load().migrate();
        assertThatThrownBy(() -> jdbc.execute(invalidSql)).isInstanceOf(DataAccessException.class);
    }

    private void legacyDatabase() {
        configuration().target("1").load().migrate();
        jdbc.execute("""
                insert into users(username,password_hash,created_at) values
                    ('alice','encoded','2026-01-01'),('bob','encoded','2026-01-01');
                insert into songs(spotify_id,title,artist,genre) values
                    ('spotify-a','Song A','Artist','rnb'),('spotify-b','Song B','Artist','rnb'),('spotify-c','Song C','Artist','rock');
                insert into song_ratings(user_id,song_id,rating,matchups_played) values (1,1,1532,1),(2,3,1500,0);
                insert into matchups(user_id,songa_id,songb_id,winner_id,genre,created_at) values (1,1,2,1,'rnb','2026-01-02');
                insert into taste_profiles(user_id,genre,archetype_key,archetype_name,created_at)
                    values (1,'rnb','RNB_DEEP_90S','Test archetype','2026-01-03');
                insert into communities(archetype_key,name) values ('RNB_DEEP_90S','Test community');
                insert into posts(community_id,author_id,body,created_at) values (1,1,'Legacy post','2026-01-02');
                insert into replies(post_id,author_id,body,pinned,created_at) values (1,1,'Legacy reply',false,'2026-01-03');
                insert into likes(user_id,post_id,created_at) values (1,1,'2026-01-04');
                """);
    }
}
