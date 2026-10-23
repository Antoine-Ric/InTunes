# Database migrations and transaction rules

Flyway owns schema creation and upgrades. Spring runs it before Hibernate's
`ddl-auto: validate`, in development and production. Hibernate no longer changes
the schema. Use PostgreSQL 16 and Java 21 for the tests.

## Fresh databases

Start the application normally. `V1` creates the original scaffold tables;
`V2` introduces runs, integrity constraints, versions, and query indexes. Flyway
records applied checksums and does nothing on the next startup. Never edit an
applied migration; add the next version instead. Flyway clean is disabled.

The datasource accepts `DATABASE_URL` (a JDBC PostgreSQL URL),
`DATABASE_USERNAME`, and `DATABASE_PASSWORD`; defaults still match Docker Compose.

## Existing local scaffold databases

Stop the old app and back up the database before upgrading. Check that it is
the nine-table Hibernate scaffold, with no Flyway history. The one-time
`baseline` Spring profile records that existing schema as version 1, then runs
V2. For example, with the project JDK selected:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=baseline
```

After that startup, stop the application and launch normally without the
`baseline` profile. Normal startup refuses an unversioned nonempty database.
Do not use this profile for an unrelated schema or a partially built database.
The application never runs migrations against the development database during
tests; tests own disposable PostgreSQL containers.

V2 preserves row IDs and existing songs, votes, ratings, profiles, boards, and
likes. It infers one historical attempt per user/genre: a profile makes it
completed; activity without a profile becomes an in-progress attempt. Existing
Elo values remain unchanged. This is the only history the old schema represents;
it cannot reconstruct retakes that were never recorded separately.

Legacy rows that violate new rules cause the entire V2 migration to roll back.
Inspect the named constraint in the error and resolve the conflicting data
deliberately before retrying. No migration deletes or silently merges history.
Typical checks before upgrading:

```sql
-- Reversed pairs count as the same pair within a legacy attempt.
SELECT user_id, genre, least(songa_id, songb_id), greatest(songa_id, songb_id), count(*)
FROM matchups GROUP BY 1, 2, 3, 4 HAVING count(*) > 1;
SELECT author_id, count(*) FROM replies WHERE pinned GROUP BY author_id HAVING count(*) > 1;
SELECT * FROM likes WHERE (post_id IS NULL) = (reply_id IS NULL);
SELECT user_id, post_id, count(*) FROM likes WHERE post_id IS NOT NULL
GROUP BY 1, 2 HAVING count(*) > 1;
SELECT user_id, reply_id, count(*) FROM likes WHERE reply_id IS NOT NULL
GROUP BY 1, 2 HAVING count(*) > 1;
SELECT id, genre FROM songs
WHERE genre NOT IN ('hiphop','rnb','afrobeats','pop','country','gospel','rock');
```

Other invalid rows, such as a winner outside the issued pair or songs assigned
to another genre, are also rejected. Database error constraint names identify
the affected rule. Restore the backup if the old application must be used again;
it cannot safely write the new required run columns.

## ADR alignment and query contracts

- ADR-0001: database genre checks accept exactly the seven launch keys.
- ADR-0002: a song has one genre and Spotify IDs remain globally unique.
- ADR-0003: `MatchupRun` owns votes and ratings. Ratings are unique per run/song;
  the same canonical pair may recur in a retake, but never within one run.
  Only one attempt per user/genre can be in progress. Finishing or abandoning
  an attempt is terminal; the rows and their history are retained.
- `TasteProfile` remains unique per user/genre, references its source run, and
  can be updated in place using `updateFromRun`. Profile building must use the
  latest completed run, under the user's lock, rather than blend attempts.
- `SongRatingRepository.findByUserAndSong`, `findByUserAndSongIn`, and
  `findTopRatedByUserAndGenre` read the latest completed run per genre. A newer
  active or abandoned attempt does not hide the completed result. If a song
  has no rating in that completed run, these queries do not use an older one.
  Finished-time ties use the greater run ID. Live arena code uses run-specific
  rating methods instead.
- `MatchupRepository.findByUserAndGenre` and `countByUserAndGenre` describe
  history across attempts. **Round counts and exhaustion must use the explicit
  run methods**, never those historical totals.
- Board pages order by created time descending, then ID descending; replies
  order by created time ascending, then ID ascending. Rating ties use song ID.
  `ReplyRepository.findPinnedReplies` projects the profile template's `id`,
  `body`, and `from` properties as a list containing zero or one entry.
- Missing previews never remove songs from pool queries or change counts,
  consistent with the decision recorded on issue #20.

## Transactions and concurrency

Use PostgreSQL's default READ COMMITTED isolation and service-owned Spring
`@Transactional` boundaries. Repository write primitives below participate in
the caller's transaction. They do not make the unfinished matchup/community
services complete; those service issues must use these rules.

| Operation | Lock and atomic boundary | Database safeguard |
|---|---|---|
| Start/retake | Lock the user with `findByIdForUpdate`; abandon an existing active attempt, flush, then create the new attempt in the same transaction. | Partial unique index on active user/genre. |
| Vote and first rating | Lock the owned run with `findByIdAndUserForUpdate` **before** reading vote/rating state. Verify active state, owner, issued pair and remaining rounds; find or create both ratings, write the vote, update both ratings and completion together. | Canonical pair unique per run, unique run/song rating, context foreign keys, optimistic versions. |
| Profile replacement | Lock user, then run; select latest completed run and create/update the unique user/genre profile in that transaction. | Unique user/genre; source-run context FK; optimistic version. |
| Community creation | `insertIfAbsent`, then `findByArchetypeKey`, in one transaction. | Unique archetype and `ON CONFLICT DO NOTHING`. |
| Likes | `likePostIfAbsent` or `likeReplyIfAbsent`; avoid check-then-insert. | Exactly one target; unique user/target; `ON CONFLICT DO NOTHING`. |
| Pin swap | Lock the author with `findByIdForUpdate`; verify target ownership, unpin current reply and **flush**, then pin the selected reply. Commit once. | Partial unique index permits at most one pin per author; optimistic reply version. |

When an operation needs both locks, acquire **user before run**. If multiple
runs are ever locked, lock them by ascending ID. Keep provider/network calls
outside locked transactions. Reading before a lock can leave stale entities in
the persistence context; lock first and read the affected rows afterwards.

An optimistic conflict, deadlock, or unexpected unique violation rolls back the
whole transaction. Retry from a new transaction after rereading state; do not
catch a failed SQL insert and keep using its aborted transaction. The explicit
upserts handle expected creation races without raising that failure. Request
idempotency and persisted issued-pair handling remain #23–#24; the pair unique
index alone is not a complete vote retry protocol.

## Verification

```bash
mvn test
mvn -Dtest=MigrationTest,RepositoryTest,ConcurrencyTest test
```

Migration tests cover fresh creation, repeat startup, explicit legacy baseline
and data preservation, rollback on conflicting legacy pairs, and invalid-row
rejection. The application and JPA tests validate mappings against the migrated
schema. Repository tests cover scoping, latest completed results, ordering ties,
pagination, missing rows, previewless songs, pin projections and idempotent
inserts. Concurrent transaction tests cover rating initialization and updates,
retakes, pin swaps, community and like creation, stale versions, and rollback of
vote/rating changes together.

References: [Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html),
[Flyway baseline](https://documentation.red-gate.com/flyway/reference/commands/baseline),
[PostgreSQL constraints](https://www.postgresql.org/docs/16/ddl-constraints.html).
