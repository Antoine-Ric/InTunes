# ADR-0002: A song belongs to exactly one genre

- **Status:** Accepted
- **Date:** 2026-10-23
- **Issue:** [#13 — Finalize genres, song membership, and run lifecycle](https://github.com/Antoine-Ric/InTunes/issues/13) (decision 2 of 3)

## Context

Plenty of songs sit on a genre border (an R&B record with pop reach, a gospel
choir on a hip hop track). The data model has to answer one question: can the
same song appear in more than one genre's matchup pool?

The shipped seeding code already leans one way: `Song` carries a single
`genre` value, and the seeder deduplicates Spotify track IDs globally, so the
same track can never be inserted twice. The alternative (a song linked to many
genres through a join table) would also force a ruling on ratings: Elo ratings
are stored per user per song, so a song battled in one genre would carry its
rating into any other genre it appears in, leaking one run's results into
another.

## Decision

**One song, one genre.** Genre stays a single value on `Song`, the seven seed
pools (ADR-0001) are curated to be disjoint, and global Spotify-ID dedupe stays
as implemented. When a song fits two genres, the curator assigns it to its
single best-fit genre; it simply does not appear in the other pool.

## Consequences

- **No schema change.** The model and seeding code from PR #4 already work
  this way; `SongRepository.findByGenre` remains the only pool query.
- **Ratings stay clean.** A song's Elo history exists inside exactly one
  genre, so there is no cross-genre rating leak to rule on.
- **Curation rule:** the seven 25-song lists must share zero songs. Every
  crossover call is made once, by a human, at seed time.
- **Accepted cost:** a user playing one genre never encounters a song filed
  under another, even if it would have fit.
- **Reversal cost (known, accepted):** moving to multi-genre membership later
  means a schema migration (join table), a seeder dedupe rework, and a ruling
  on whether ratings are per song or per song-in-genre. Doable, not free.
