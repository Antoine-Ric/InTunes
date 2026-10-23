# ADR-0001: Launch genre catalog

- **Status:** Accepted
- **Date:** 2026-10-23
- **Issue:** [#13 — Finalize genres, song membership, and run lifecycle](https://github.com/Antoine-Ric/InTunes/issues/13) (decision 1 of 3)

## Context

The genre list is a load-bearing decision: it sizes the seed pools (25 songs per
genre), the archetype table (genres × mainstream/deep × 3 era buckets), and the
number of communities the explore page can show. Until it was locked, seeding,
the archetype name table, and the genre picker UI all had to guess.

Two earlier drafts existed and disagreed:

- The scaffold code named 6 genres: hiphop, rnb, afrobeats, pop, rock, latin.
- The original MVP scope said 5 genres × 25 songs.

## Decision

InTunes launches with **7 genres**:

| Key (as used in code/URLs) | Display name |
|---|---|
| `hiphop` | Hip Hop |
| `rnb` | R&B |
| `afrobeats` | Afrobeats |
| `pop` | Pop |
| `country` | Country |
| `gospel` | Gospel |
| `rock` | Rock |

Changes from the scaffold list: **country** and **gospel** are added;
**latin** is dropped from the launch catalog. Keys stay lowercase single tokens,
matching the existing scaffold convention (`hiphop`, not `hip-hop`).

## Consequences

- **Seeding:** 7 genres × 25 songs = **175 songs** to curate and seed for launch.
- **Archetype table:** 7 genres × 2 depth buckets × 3 era buckets = **42 named
  archetypes**, and therefore up to 42 communities. Many will be empty at
  launch; that is acceptable for MVP, and the explore page must handle sparse
  communities gracefully.
- **Archetype keys** keep the existing format (`GENRE_DEPTH_ERA`, e.g.
  `HIPHOP_DEEP_90S`); the new genres slot in without a format change.
- **Changing the catalog later** (adding latin back, dropping a genre) requires
  a new ADR superseding this one, plus seeding work and archetype names for the
  change. Removing a genre that users already hold profiles in needs a data
  decision at that time; it is not handled here.

## Still open from issue #13

These get their own ADRs when decided:

- **ADR-0002 (pending):** whether a song can compete in more than one genre.
- **ADR-0003 (pending):** whether a user gets one lifetime run per genre or can
  retake a genre.
