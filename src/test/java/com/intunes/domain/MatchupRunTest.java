package com.intunes.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchupRunTest {
    private final User alice = new User("alice", "encoded", "Alice");

    @ParameterizedTest
    @EnumSource(value = MatchupRun.Status.class, names = {"COMPLETED", "ABANDONED"})
    void finishingARunIsTerminal(MatchupRun.Status status) {
        MatchupRun run = new MatchupRun(alice, "rnb");
        assertThat(run.getStatus()).isEqualTo(MatchupRun.Status.IN_PROGRESS);
        assertThat(run.getFinishedAt()).isNull();
        if (status == MatchupRun.Status.COMPLETED) run.complete();
        else run.abandon();
        assertThat(run.getStatus()).isEqualTo(status);
        assertThat(run.getFinishedAt()).isAfterOrEqualTo(run.getStartedAt());
        assertThatThrownBy(run::complete).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(run::abandon).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void profileRequiresACompletedRun() {
        MatchupRun run = new MatchupRun(alice, "rnb");
        assertThatThrownBy(() -> new TasteProfile(run, "RNB_DEEP_90S", "Test"))
                .isInstanceOf(IllegalArgumentException.class);
        run.abandon();
        assertThatThrownBy(() -> new TasteProfile(run, "RNB_DEEP_90S", "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void profileCannotChangeOwnerOrGenreEvenWhenOwnersHaveNoIdsYet() {
        MatchupRun original = new MatchupRun(alice, "rnb");
        original.complete();
        TasteProfile profile = new TasteProfile(original, "RNB_DEEP_90S", "Test");
        MatchupRun otherOwner = new MatchupRun(new User("bob", "encoded", null), "rnb");
        otherOwner.complete();
        MatchupRun otherGenre = new MatchupRun(alice, "country");
        otherGenre.complete();
        assertThatThrownBy(() -> profile.updateFromRun(otherOwner, "RNB_DEEP_90S", "Test"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.updateFromRun(otherGenre, "COUNTRY_DEEP_90S", "Test"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(profile.getRun()).isSameAs(original);
    }

    @Test
    void ratingRequiresASongFromTheRunsGenre() {
        MatchupRun run = new MatchupRun(alice, "gospel");
        Song song = new Song("track", "Song", "Artist", null, null, "rock", 50, 1990);
        assertThatThrownBy(() -> new SongRating(run, song)).isInstanceOf(IllegalArgumentException.class);
    }
}
