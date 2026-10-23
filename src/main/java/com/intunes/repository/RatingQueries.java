package com.intunes.repository;

/** Share the latest-completed-run rule across profile rating queries (ADR-0003). */
final class RatingQueries {
    private RatingQueries() { }

    static final String LATEST_COMPLETED = """
            r.run.status = 'COMPLETED' and not exists (
                select newer.id from MatchupRun newer
                where newer.user = r.user and newer.genre = r.genre and newer.status = 'COMPLETED'
                  and (newer.finishedAt > r.run.finishedAt or
                      (newer.finishedAt = r.run.finishedAt and newer.id > r.run.id)))
            """;
}
