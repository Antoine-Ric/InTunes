package com.intunes.service;

/** Domain failures that the arena controller can translate when it is wired in #25. */
public class MatchupException extends RuntimeException {
    public enum Reason { INVALID_USER, INVALID_GENRE, INSUFFICIENT_SONGS, RUN_NOT_FOUND, RUN_ABANDONED, PAIR_STATE_CONFLICT }
    private final Reason reason;

    public MatchupException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
