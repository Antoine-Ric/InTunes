# ADR-0003: Retakes are supported; every run has its own identity

- **Status:** Accepted
- **Date:** 2026-10-23
- **Issue:** [#13 — Finalize genres, song membership, and run lifecycle](https://github.com/Antoine-Ric/InTunes/issues/13) (decision 3 of 3)

## Context

A "run" is one user's pass through one genre: 16 rounds of matchups that end
in a named archetype profile. The question: after finishing a genre, can the
user take it again, or is the first result permanent?

If retakes were ever added on top of a model that assumes one run, almost
everything breaks at once: votes from two attempts mix together, the
"never repeat a pair" rule can't tell which attempt a pairing belongs to,
ratings have nowhere clean to restart from, and the profile can't say which
attempt it describes. Scoping by run from the start is cheap; retrofitting it
is not.

## Decision

**Retakes are supported, and run identity is modeled from the start.**

- Introduce an explicit **run** concept: one user, one genre, one attempt,
  with a start and a completion state.
- **Votes, ratings, pair uniqueness, and round counts are scoped by run**,
  not just by user + genre. A retake starts fresh: ratings begin at 1500 and
  any pairing may appear again. Within a single run, a pairing still never
  repeats.
- **The taste profile reflects the user's latest completed run** for that
  genre. Rebuilding a profile reads only that run's results; the profile
  stays unique per user + genre and is overwritten in place.
- A user has **at most one in-progress run per genre**. Starting a new run
  while one is unfinished abandons the unfinished one.
- Old runs' data is **retained** (it is the user's history), just inactive.

Whether the MVP interface surfaces a "retake this genre" action is a product
call that can change either way; the data model supports it with no schema
change.

## Consequences

- **Schema:** a run entity (or run ID carried on the matchup and rating
  records) joins the model before matchup persistence is finalized. Rating
  uniqueness becomes per (run, song) rather than per (user, song).
- **MatchupService:** `nextPair`, `recordResult`, and `isComplete` all take
  the current run as context; the existing design notes for `nextPair` need a
  run-scoping pass before implementation.
- **Profile building:** TasteProfileService reads the latest completed run
  only; it must never blend two runs' ratings.
- **Accepted cost:** slightly more bookkeeping in every matchup query, in
  exchange for never having to migrate vote/rating history if retakes ship.
