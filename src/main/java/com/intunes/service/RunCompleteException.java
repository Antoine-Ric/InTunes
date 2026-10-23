package com.intunes.service;

/** Normal end-of-run signal. Completing a run commits even when nextPair returns this signal. */
public class RunCompleteException extends RuntimeException {
    private final Long runId;

    public RunCompleteException(Long runId) {
        super("Run is complete; start an explicit retake to play this genre again");
        this.runId = runId;
    }

    public Long getRunId() { return runId; }
}
