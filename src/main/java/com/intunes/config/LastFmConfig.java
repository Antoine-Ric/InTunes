package com.intunes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds intunes.lastfm.* from application.yml.
 * Register an API account at https://www.last.fm/api/account/create to get these.
 * Only the API key is needed for read-only calls; the shared secret is only
 * for signed/authenticated calls, which InTunes does not make.
 */
@ConfigurationProperties(prefix = "intunes.lastfm")
public record LastFmConfig(String apiKey, String sharedSecret, String baseUrl) {
}
