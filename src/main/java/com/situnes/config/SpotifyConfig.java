package com.situnes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds situnes.spotify.* from application.yml.
 * Register an app at https://developer.spotify.com/dashboard to get these.
 * TODO: implement the client-credentials token exchange in SpotifyClient.
 */
@ConfigurationProperties(prefix = "situnes.spotify")
public record SpotifyConfig(String clientId, String clientSecret) {
}
