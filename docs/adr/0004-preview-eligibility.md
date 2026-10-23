# ADR-0004: Songs without previews stay eligible, with an honest unavailable state

- **Status:** Accepted
- **Date:** 2026-10-23
- **Issue:** [#20 — Define preview eligibility consistently across the app](https://github.com/Antoine-Ric/InTunes/issues/20)

## Context

Song previews come from the iTunes Search API, and coverage is not total:
some tracks in a genre pool will have no 30-second preview (`previewUrl`
is null). Spotify is not a fallback; it removed preview URLs for new apps
in November 2024.

The alternative was to exclude no-preview songs from pairing entirely. That
shrinks pools unevenly (a genre's 25 songs might quietly become 19),
distorts the exhaustion math, and punishes songs for a data gap that has
nothing to do with whether users know and love them.

## Decision

**A missing preview never makes a song ineligible.** The song competes
exactly like any other; where its preview would play, the app shows an
honest **"Preview unavailable"** state instead. No fake player, no silent
empty audio element, no pretending audio exists.

This one policy applies everywhere the question comes up:

- **Seeding:** a song is accepted or skipped for the existing reasons only
  (duplicate, not found, provider failure). A null `previewUrl` is never a
  skip reason, and no preview-coverage threshold gates a genre pool or a
  launch. Coverage may be *reported* (it is useful to know), never enforced.
  Curators may prefer preview-having songs when hand-picking lists, but
  that is a human curation preference, not a system rule.
- **Pairing:** `nextPair` never filters on `previewUrl`. Pool size,
  pair-uniqueness, and exhaustion are computed over all songs in the genre
  (per ADR-0001 and ADR-0002).
- **Completion:** round counts and run completion (ADR-0003) are unaffected
  by how many songs in the run had previews.
- **Interface:** any surface that would play a preview (the arena first,
  profile top-songs if it plays audio) renders the same unavailable state
  for a null preview. The song stays fully votable; the user judges it by
  title, artist, and album art.
- **Tests:** pairing tests must include a null-preview song in the pool and
  assert it can be served; UI acceptance must cover the unavailable state
  rendering.

## Consequences

- **No code change required in the seeder:** SongSeedingService already
  persists songs with a null `previewUrl`; this ADR makes that behavior
  policy rather than accident.
- **MatchupService design constraint:** the pairing design notes already
  assume this ("don't filter songs out for missing previews"); filtering on
  preview in any future query or service is a policy violation, not a
  tuning choice.
- **Accepted cost:** some arena rounds are judged from memory instead of a
  30-second clip. That is the honest trade for pools that reflect the genre
  rather than iTunes' catalog gaps.
