# Matchup pairing and resume contract

Implements [#22](https://github.com/Antoine-Ric/InTunes/issues/22) and
[#23](https://github.com/Antoine-Ric/InTunes/issues/23). This replaces the missing
`hidden_files/intunes-nextpair-design-notes.md` reference.

## Run identity and eligibility

Every attempt has a `MatchupRun`, following [ADR-0003](adr/0003-run-lifecycle-retakes.md).
Votes, ratings, issued identities, exposure, and pair uniqueness belong to that
run. Historical totals across a user's attempts never determine its round.

All songs in the selected genre are eligible, including songs with null or
blank preview URLs. Preview coverage does not change pool size, round counts,
or exhaustion. The supported genre keys are those in ADR-0001; a song remains
in exactly one genre under ADR-0002.

Fewer than two songs produces `MatchupException` with reason
`INSUFFICIENT_SONGS`; no new attempt is created. Unsupported genres, missing
users, foreign run IDs and abandoned attempts also have explicit domain errors.

## Selecting a pair

The round is the current run's persisted vote count plus one. A run snapshots
`intunes.matchup.rounds-per-genre` when created, defaulting to 16. Existing runs
gain the default 16-round target in V3. Later configuration changes affect new
attempts, rather than changing a resumed attempt's target.

1. Enumerate unordered pairs from the genre pool, sorted by song ID.
2. Remove every pair already voted on in this run. `(A, B)` and `(B, A)` are
   the same pair. Songs may appear again against different opponents.
3. Count each song's exposure from its appearances in the run's votes. Songs
   with no votes have exposure zero. Merely displaying or refreshing a pair
   does not increase exposure.
4. Choose an anchor among songs that still have an available opponent,
   prioritizing the lowest exposure.
5. In rounds **1–4**, randomize among equally least-exposed anchors, then among
   that anchor's least-exposed available opponents. Early selection is random
   while giving unseen songs a chance before repeatedly showing others.
6. In rounds **5+**, choose the lowest-ID least-exposed anchor, then its
   nearest-rated unused opponent. Missing rating rows use
   `intunes.matchup.elo-start-rating`, default 1500. Equal rating gaps prefer
   the less-exposed opponent, then the lower song ID.

The anchor/exposure priority prevents a high-rated favorite from always being
the starting point. Equal-rating pools have deterministic Swiss tie-breaking;
tests verify balanced exposure through a 16-round, 25-song run. With differing
ratings, closeness takes priority when choosing the anchor's opponent; there
is no universal hard exposure cap that could block otherwise available pairs.

Selection enumerates at most `n × (n − 1) / 2` pairs. For 25 songs, that is
300 candidates. It never retries random choices until one happens to be unused.

## Issued identity and refreshes

`issued_matchup_pairs` stores the selected songs and orientation, run, owner,
genre, round, issue time, and UUID. Status is `ISSUED` or `VOTED`; a voted pair
references its persisted vote and resolution time. Database rules allow only
one pending pair and one issued identity per round in each run, and prohibit
issuing the same unordered pair twice in that run.

`nextPair(user, genre)` resumes an active attempt or creates the first attempt.
It returns the existing unanswered pair on every refresh, including after a
service/process restart. The returned `MatchupPair` contains `runId` and
`issuedPairId` for future vote validation. Displaying a pair creates no vote or
rating rows and does not advance the round.

`nextPair(user, runId)` resumes that exact owned run; it never silently switches
a stale client to a newer attempt. A UUID is an identifier, not authorization:
the future vote handler must also validate the authenticated owner, run, songs,
pending status and round under the run lock.

When a matching vote has already been saved, the next read reconciles its
pending identity to `VOTED` before issuing the next round. Future vote recording
in #24 should call `IssuedMatchupPair.markVoted` in the same transaction as the
vote and both Elo updates. The reconciliation path also handles already-saved
votes; it does not implement a public vote-submission or retry protocol.

## Completion and retakes

Both `nextPair` and `isComplete` use the same pool and unordered vote history.
A run completes when its vote count reaches its stored target **or** every
possible pair in its current genre pool has been voted on. A two-song pool
therefore finishes after one vote; a three-song pool after three votes.

An unanswered issued pair is still available and cannot exhaust a run by
itself. `isComplete` persists completion and returns true. If `nextPair` detects
completion, it persists it and throws `RunCompleteException`; that exception
explicitly does not roll the transaction back. Controller redirects are #25.

After completion, `nextPair(user, genre)` reports that the run is complete;
refreshing does not automatically start a retake. `startNewRun(user, genre)` is
the explicit retake operation. It abandons an unfinished attempt if necessary,
starts a fresh one, and retains the old votes, ratings and issued identities.
Pairs may recur in the new run. Old pending identities belong to the abandoned
attempt and cannot be resumed through the owned-run API.

## Skip decision

**Live skipping is deferred.** The live arena does not render a skip button,
and no live skip endpoint is provided. An unanswered live pair stays in place
until voted on or the user explicitly abandons the attempt by starting a new
one. No skip changes the round counter, makes a pair repeatable, or creates an
"all remaining pairs skipped" exhaustion state. The separately labeled demo
retains its existing local skip behavior.

Adding live skipping requires defining persisted skip state, round counting,
repeatability and exhaustion together, rather than advancing only the browser.

## Transactions and remaining integration

Each issuing/completion/retake operation owns a Spring transaction. It locks
the user first and the owned run second, consistent with
[persistence rules](PERSISTENCE.md). Parallel first requests therefore create
one run and one pending pair; parallel refreshes return the same UUID. Unique
indexes and optimistic versions provide additional database protection.

Pair selection and resume are implemented here. Atomic, authenticated,
idempotent vote recording remains #24. Controller/current-user wiring, live
DTO/template metadata alignment, and completion redirects remain #25; browser
arena integration remains #34. The existing `recordResult` method continues
to identify #24 rather than accepting partially validated votes.

## Verification

```bash
mvn -Dtest=PairingPolicyTest,MatchupServiceTest test
mvn test
```

Pure policy tests cover early random exposure, Swiss nearest-rating selection,
stable ties, configured default ratings, unordered non-repetition and bounded
exhaustion. PostgreSQL service tests cover persistent refresh/resume across
service instances, full runs, completion commits, small previewless pools,
explicit retakes, owner/genre isolation and simultaneous issuing requests.
Migration tests cover upgrading existing V2 databases and issued-pair constraints.
