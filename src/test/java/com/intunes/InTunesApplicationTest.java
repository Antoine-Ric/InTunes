package com.intunes;

import com.intunes.service.ITunesClient;
import com.intunes.service.LastFmClient;
import com.intunes.service.SongSeedingService;
import com.intunes.service.SpotifyClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
// Close Spring's datasource before Testcontainers removes PostgreSQL.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false",
        "intunes.spotify.client-id=",
        "intunes.spotify.client-secret=",
        "intunes.lastfm.api-key=",
        "intunes.lastfm.shared-secret="
})
class InTunesApplicationTest {

    // Each test run owns an isolated database; no local .env or provider calls are needed.
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("intunes_context_test")
            .withUsername("intunes_test")
            .withPassword("intunes_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestRestTemplate http;

    @Test
    void contextStartsWithPostgresAndRealProviderBeans() throws SQLException {
        assertThat(context.getBean(ITunesClient.class)).isNotNull();
        assertThat(context.getBean(LastFmClient.class)).isNotNull();
        assertThat(context.getBean(SpotifyClient.class)).isNotNull();
        assertThat(context.getBean(SongSeedingService.class)).isNotNull();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .isEqualTo("intunes_context_test");
        assertThat(jdbc.queryForObject("select count(*) from songs", Long.class)).isZero();
    }

    @Test
    void homePageIsServedAfterStartup() {
        var response = http.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("InTunes");
    }
}
