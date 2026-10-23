package com.intunes.dto;

/** One song to seed into a genre pool: just a title and artist to look up. */
public record SeedTrack(String title, String artist) {
}
