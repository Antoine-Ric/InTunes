# InTunes — MVP Checklist

*Where you are and everything left, in build order. Reviewed Oct 23, 2026, against the current code, including the seeding clients, local startup guide, and expanded Elo tests.*

**Track on GitHub:** [MVP master tracker #6](https://github.com/Antoine-Ric/InTunes/issues/6) · [MVP milestone](https://github.com/Antoine-Ric/InTunes/milestone/1) · [Issue index and workflow](intunes-mvp-issues.md). Every checklist item links to its delivery issue below; issue labels are the current source of status.

**MVP is done when:** a stranger can open the live URL, register, pick a seeded genre, complete 16 head-to-head rounds, play real previews when available with an explicit unavailable-audio state otherwise, receive and share a named archetype profile, land in that archetype's community board, post / reply / like / pin, and browse all communities on the explore page. The normal seeded path must support all 16 rounds; small-pool exhaustion is a separately tested edge case.

---

## ✅ Already done

- [x] Spring Boot + Postgres + Thymeleaf app, docker-compose for local Postgres — [#7](https://github.com/Antoine-Ric/InTunes/issues/7)
- [x] All 9 entities scaffolded (User, Song, SongRating, Matchup, TasteProfile, Community, Post, Reply, Like) — [#7](https://github.com/Antoine-Ric/InTunes/issues/7)
- [x] All pages built as real Thymeleaf templates (index, genres, matchup, profile, community, explore) + design-system CSS — [#8](https://github.com/Antoine-Ric/InTunes/issues/8)
- [x] Frontend JS written API-first (arena.js, community.js) with demo fallback — [#8](https://github.com/Antoine-Ric/InTunes/issues/8)
- [x] EloRatingService + 21 test cases, including precision, symmetry, extreme rating gaps, and repeated matchups — [#9](https://github.com/Antoine-Ric/InTunes/issues/9)
- [x] Seeding stack (PR #4): SpotifyClient, ITunesClient, LastFmClient, SongSeedingService + 13 tests — [#10](https://github.com/Antoine-Ric/InTunes/issues/10)
- [x] Song entity finished (constructors, getters/setters) + SongRepository queries (findByGenre, dedupe checks) — [#10](https://github.com/Antoine-Ric/InTunes/issues/10)
- [x] Full current unit suite: 34 passing tests (21 Elo + 13 client/seeding tests); provider calls in these tests are mocked — [#9](https://github.com/Antoine-Ric/InTunes/issues/9)
- [x] Local startup guide in `docs/RUN_LOCALLY.md`, linked from the README; local VS Code launch configuration loads `.env` — [#11](https://github.com/Antoine-Ric/InTunes/issues/11)
- [x] Verify application startup with PostgreSQL and Spring dependency injection after the seeding changes: full web context, real provider beans, database/schema query, and HTTP 200 from `/` verified; fix and regression tests delivered on `main` — [#12](https://github.com/Antoine-Ric/InTunes/issues/12)
- [ ] Verify configured credentials with live Spotify, iTunes, and Last.fm requests and inspect the returned metadata/previews; mocked client tests do not establish live API access — [#17](https://github.com/Antoine-Ric/InTunes/issues/17)

---

## Phase 1 — Persistence foundations

Complete the application-facing entity APIs and persistence rules before building the core loop. The entities use JPA field access; getters/setters are needed by application code and templates, not for JPA to persist annotated fields.

- [ ] Add appropriate constructors and accessors for **User, SongRating, Matchup, TasteProfile, Community, Post, Reply, Like**, preserving a public or protected no-argument constructor for JPA (Song is done) — [#14](https://github.com/Antoine-Ric/InTunes/issues/14)
- [ ] Introduce versioned database migrations (Flyway or Liquibase), including a baseline for existing local databases and creation of a fresh database; production `ddl-auto: validate` must run after migrations — [#15](https://github.com/Antoine-Ric/InTunes/issues/15)
- [ ] Add database constraints and query indexes: unique likes per user/target, exactly one of post/reply on a Like, and canonical pair uniqueness within the chosen user/genre/run scope; preserve existing user/song-rating/profile/community uniqueness rules — [#15](https://github.com/Antoine-Ric/InTunes/issues/15)
- [ ] Choose transaction and concurrency rules for vote recording, first-time rating creation, community creation, and pin swaps; use locking/versioning plus database constraints so simultaneous requests preserve invariants — [#15](https://github.com/Antoine-Ric/InTunes/issues/15)
- [ ] `MatchupRepository`: add `findByUserAndGenre` and `countByUserAndGenre` — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] `SongRatingRepository`: add `findByUserAndSong`, a "ratings for these songs" query (`findByUserAndSongIn`), and a top-rated query for the profile page (user + genre, order by rating desc) — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] `TasteProfileRepository`: add `findByUserAndGenre` — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] `CommunityRepository`: add `findByArchetypeKey` — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] `PostRepository`: add a paginated board query (community, newest first, `Pageable`) — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] `ReplyRepository`: add `findByPost` (oldest first) and a way to find a user's currently pinned reply; adapt the result to the profile template's `pinnedReplies` list — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] **Create `LikeRepository`** (it doesn't exist yet): `existsByUserAndPost` for idempotent likes, `countByPost` for like counts — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] `UserRepository`: add `findByUsername` (auth needs it later; add it now while you're in there) — [#16](https://github.com/Antoine-Ric/InTunes/issues/16)
- [ ] Lock the genre list: the scaffold comment names 6 (hiphop, rnb, afrobeats, pop, rock, latin); the original MVP scope said 5 genres × 25 songs. Pick the final list, it drives seeding and the archetype table below — [#13](https://github.com/Antoine-Ric/InTunes/issues/13)
- [ ] Decide whether a song can compete in multiple genres. The current model assigns one genre and globally deduplicates Spotify IDs; either curate disjoint pools or change the song/genre relationship and rating uniqueness before seeding — [#13](https://github.com/Antoine-Ric/InTunes/issues/13)
- [ ] Decide whether MVP supports one lifetime run per user/genre or retakes. If retakes are supported, model run identity and scope votes, ratings, pair uniqueness, and profile rebuilding accordingly — [#13](https://github.com/Antoine-Ric/InTunes/issues/13)

## Phase 2 — Seeding goes live (get real songs in the DB)

- [ ] Choose a controlled seeding entry point: an operator-only command, or `AdminSeedController` with `POST /api/admin/seed/{genre}` and a seed-from-tag endpoint returning `SeedReport` JSON — [#18](https://github.com/Antoine-Ric/InTunes/issues/18)
- [ ] If using HTTP endpoints, require admin authorization from their introduction; ordinary login is insufficient. Until real auth exists, use the operator-only command rather than exposing unprotected seed endpoints — [#18](https://github.com/Antoine-Ric/InTunes/issues/18)
- [ ] Curate and commit reproducible seed lists: 25 songs per genre (draft from Last.fm tag charts via `draftSeedList`, then review the selections) — [#21](https://github.com/Antoine-Ric/InTunes/issues/21)
- [ ] Configure provider connection/read timeouts and bounded retries for transient/rate-limit failures, honoring retry delays where supplied; report failed candidates so an operator can retry them — [#19](https://github.com/Antoine-Ric/InTunes/issues/19)
- [ ] Verify audio identity: when iTunes has no matching artist/title, return no preview or require manual review instead of silently using its first unrelated result — [#19](https://github.com/Antoine-Ric/InTunes/issues/19)
- [ ] Define eligibility for songs with missing previews: retain with an honest unavailable state or exclude from pairing. Apply the same policy in seed checks, pairing, completion counts, and acceptance tests — [#20](https://github.com/Antoine-Ric/InTunes/issues/20)
- [ ] Run the seeds locally for every genre — [#21](https://github.com/Antoine-Ric/InTunes/issues/21)
- [ ] Verify in the DB: song count per genre, `previewUrl` coverage, spot-check a few rows (album art, release year, popularity all sane) — [#21](https://github.com/Antoine-Ric/InTunes/issues/21)
- [ ] Confirm every genre clears the minimum after eligibility checks: 25 eligible songs = 300 possible unordered pairs, enough for the normal 16-round run — [#21](https://github.com/Antoine-Ric/InTunes/issues/21)

## Phase 3 — MatchupService (the core loop)

The previously referenced `hidden_files/intunes-nextpair-design-notes.md` is absent from this checkout. Keep the pairing design in a tracked document under `docs/` before relying on it.

- [ ] `nextPair(user, genre)`: rounds 1–4 random pairs; rounds 5+ Swiss-style (nearest-rated unpaired song); a pairing never repeats in either order; round = user's genre matchup count + 1; totalRounds = 16 from `intunes.matchup.rounds-per-genre` — [#22](https://github.com/Antoine-Ric/InTunes/issues/22)
- [ ] Define song exposure and tie-breaking so equally rated songs have a predictable selection rule and a few songs do not monopolize the run — [#22](https://github.com/Antoine-Ric/InTunes/issues/22)
- [ ] Preserve the issued pair across refresh/resume until voted on or explicitly skipped; store or validate its identity on the server — [#23](https://github.com/Antoine-Ric/InTunes/issues/23)
- [ ] `recordResult(user, winnerId, loserId, genre)`: persist the Matchup, lazy-create SongRating rows using `intunes.matchup.elo-start-rating`, run `EloRatingService.update()`, save both ratings, and increment `matchupsPlayed` in one transaction — [#24](https://github.com/Antoine-Ric/InTunes/issues/24)
- [ ] Validate votes: authenticated user, supported genre, two distinct existing eligible songs in that genre, pair actually issued to this user/run, and run still open — [#24](https://github.com/Antoine-Ric/InTunes/issues/24)
- [ ] Make vote submission idempotent: duplicate clicks, retries after a timeout, reversed/replayed pairs, and simultaneous tabs must not double-count a vote or lose a rating change; reject stale/conflicting submissions cleanly — [#24](https://github.com/Antoine-Ric/InTunes/issues/24)
- [ ] Define skip behavior: whether it affects the round counter, whether a skipped pair can return, and what happens when all remaining pairs are skipped. Wire a live skip action or remove the live skip button — [#23](https://github.com/Antoine-Ric/InTunes/issues/23)
- [ ] `isComplete(user, genre)`: true when the count hits 16 **or** every possible pair is exhausted — [#22](https://github.com/Antoine-Ric/InTunes/issues/22)
- [ ] Edge cases: genre with fewer than 2 eligible songs returns a clean domain error; completion and pair exhaustion use the same eligibility and skip rules as `nextPair` — [#22](https://github.com/Antoine-Ric/InTunes/issues/22)
- [ ] Tests, same style as the Elo tests: fresh user gets two distinct songs at round 1; pairs get rating-closer after results exist; no pair ever repeats across a full run; single-song genre signals empty — [#22](https://github.com/Antoine-Ric/InTunes/issues/22)
- [ ] Integration tests: refresh/resume, user/genre isolation, custom starting rating, atomic rollback on failure, concurrent votes, duplicate submissions, invalid song IDs, and rejection after completion — [#24](https://github.com/Antoine-Ric/InTunes/issues/24)
- [ ] Wire `MatchupController.vote` to `recordResult` (it currently throws) — [#25](https://github.com/Antoine-Ric/InTunes/issues/25)
- [ ] Temporary current-user: use a persisted demo user behind a local-only profile until auth lands; ensure production cannot enable it — [#25](https://github.com/Antoine-Ric/InTunes/issues/25)
- [ ] Align `SongDto` with the live arena template: add `popularity` and `releaseYear`, or remove those template reads; test DTO mapping and actual template rendering — [#25](https://github.com/Antoine-Ric/InTunes/issues/25)
- [ ] Wire `PageController` `/matchup`: add `pair`, `genre`, and display attributes; when complete, redirect to `/profile` with the selected genre so multiple genre profiles remain distinct — [#25](https://github.com/Antoine-Ric/InTunes/issues/25)

## Phase 4 — TasteProfileService (the payoff screen)

- [ ] `buildProfile(user, genre)`: take the user's top 5 songs by Elo in that genre — [#26](https://github.com/Antoine-Ric/InTunes/issues/26)
- [ ] Define eligibility and ordering for the top five: include/exclude unplayed songs, break rating ties deterministically, and handle fewer than five rated songs after small-pool exhaustion — [#26](https://github.com/Antoine-Ric/InTunes/issues/26)
- [ ] Popularity bucket: average popularity ≥ 70 → `MAINSTREAM`, under 70 → `DEEP` — [#26](https://github.com/Antoine-Ric/InTunes/issues/26)
- [ ] Era bucket from the median release year of the top 5: ≥ 2015 → `NOW`, 2000–2014 → `2000S`, before 2000 → `90S` — [#26](https://github.com/Antoine-Ric/InTunes/issues/26)
- [ ] Define missing-metadata handling for popularity/release year, the median rule for an even number of songs, and the result when no usable metadata remains; avoid crashes or silently treating unknown values as zero — [#26](https://github.com/Antoine-Ric/InTunes/issues/26)
- [ ] Write the archetype name table (Map or enum): one human name for every finalized genre × depth × era combination (5 genres → 30 names; 6 → 36). Example: `HIPHOP_DEEP_90S` → "Boom Bap Archaeologist". Use one canonical key format across URLs, templates, and services — [#27](https://github.com/Antoine-Ric/InTunes/issues/27)
- [ ] Persist the TasteProfile (unique per user + genre); rebuilding must update in place, not duplicate or throw — [#28](https://github.com/Antoine-Ric/InTunes/issues/28)
- [ ] Guard: building a profile before the run is complete fails cleanly — [#28](https://github.com/Antoine-Ric/InTunes/issues/28)
- [ ] Tests: bucket boundaries (popularity exactly 70, median year on the 2015/2000 edges), name lookup for a known key, persist + rebuild idempotency — [#28](https://github.com/Antoine-Ric/InTunes/issues/28)
- [ ] Tests: missing metadata, top-five ties, unplayed songs, fewer than five songs, all archetype names covered, and user/genre isolation — [#28](https://github.com/Antoine-Ric/InTunes/issues/28)
- [ ] Wire `PageController` `/profile`: `profile` + `topSongs` attributes (the `pinnedReplies` list comes in Phase 5); provide ranked-song view data containing title, artist, release year, and rating — [#28](https://github.com/Antoine-Ric/InTunes/issues/28)

## Phase 5 — CommunityService (the boards)

- [ ] `resolveCommunity(archetypeKey)`: find-or-create the Community; call it when a profile is built so the community exists before anyone visits — [#29](https://github.com/Antoine-Ric/InTunes/issues/29)
- [ ] Add `findAll()` (the explore page TODO calls it; it doesn't exist yet) — [#29](https://github.com/Antoine-Ric/InTunes/issues/29)
- [ ] Board read: paginated posts for a community, newest first — [#29](https://github.com/Antoine-Ric/InTunes/issues/29)
- [ ] `createPost`: validate body (not blank, ≤ 2000 chars), persist — [#30](https://github.com/Antoine-Ric/InTunes/issues/30)
- [ ] `createReply`: validate body (not blank, ≤ 2000 chars) and persist a reply on an existing post — [#30](https://github.com/Antoine-Ric/InTunes/issues/30)
- [ ] `likePost`: idempotent — a second like from the same user is ignored, not an error and not a double count — [#31](https://github.com/Antoine-Ric/InTunes/issues/31)
- [ ] Decide whether unlike and unpin are in MVP. If supported, add explicit idempotent actions and persisted state/count responses; otherwise remove the frontend toggle behavior. Also decide whether liking replies is supported or deferred (the Like entity supports both targets) — [#31](https://github.com/Antoine-Ric/InTunes/issues/31)
- [ ] `pinReply`: verify the reply is the user's own, unpin their previous pinned reply, pin the new one (one pin per user) — [#31](https://github.com/Antoine-Ric/InTunes/issues/31)
- [ ] Validate nested resources: the post/reply must belong to the community in the URL. Decide whether all logged-in users may participate or only members, then enforce and test that policy — [#30](https://github.com/Antoine-Ric/InTunes/issues/30)
- [ ] Wire all four `CommunityController` endpoints (they currently throw) — [#30](https://github.com/Antoine-Ric/InTunes/issues/30)
- [ ] Wire `PageController` `/community/{archetypeKey}` (community + page of posts) and `/explore` (communities list) — [#29](https://github.com/Antoine-Ric/InTunes/issues/29)
- [ ] Wire the `pinnedReplies` list on `/profile` (zero or one item under the one-pin rule); remove demo examples that imply multiple pins or pinning someone else's reply — [#31](https://github.com/Antoine-Ric/InTunes/issues/31)
- [ ] Tests: find-or-create, post validation limits, like idempotency, pin swap + ownership check — [#31](https://github.com/Antoine-Ric/InTunes/issues/31)
- [ ] Tests: reply validation, concurrent likes/pin swaps, invalid IDs, cross-community resource access, and read/write authorization — [#31](https://github.com/Antoine-Ric/InTunes/issues/31)

## Phase 6 — Real auth (replace the stub user)

- [ ] `PasswordEncoder` bean (BCrypt); registration stores the hash only, never plaintext — [#32](https://github.com/Antoine-Ric/InTunes/issues/32)
- [ ] `UserDetailsService` backed by `UserRepository.findByUsername` — [#32](https://github.com/Antoine-Ric/InTunes/issues/32)
- [ ] Registration validation: username normalization and uniqueness, required fields, password limits compatible with the encoder, and friendly duplicate-user errors — [#32](https://github.com/Antoine-Ric/InTunes/issues/32)
- [ ] Rewrite `SecurityConfig`: form login; protect `/api/**`, `/matchup`, `/profile`, `/community/**`; require admin privileges for `/api/admin/**`; leave `/`, `/genres`, `/explore`, registration, login, static assets, and any approved public-profile route public — [#32](https://github.com/Antoine-Ric/InTunes/issues/32)
- [ ] Keep CSRF protection for session-authenticated mutations; render the token and send it in arena/community `fetch` headers, including any unlike/unpin actions — [#33](https://github.com/Antoine-Ric/InTunes/issues/33)
- [ ] Register + login Thymeleaf pages, plus logout — [#32](https://github.com/Antoine-Ric/InTunes/issues/32)
- [ ] Replace the stub user everywhere: MatchupController, CommunityController, and PageController resolve the real logged-in user (`Principal` / `@AuthenticationPrincipal`) — [#32](https://github.com/Antoine-Ric/InTunes/issues/32)
- [ ] Manual test: register → log in → vote → confirm the ratings land under *your* user row — [#32](https://github.com/Antoine-Ric/InTunes/issues/32)
- [ ] Automated security tests: unauthenticated writes rejected, ordinary users cannot seed, one user cannot modify another's data, valid/invalid CSRF handling, and logout/session behavior — [#33](https://github.com/Antoine-Ric/InTunes/issues/33)

## Phase 7 — Full UI wiring and polish

- [ ] `PageController` `/genres`: add the `genres` model attribute — [#34](https://github.com/Antoine-Ric/InTunes/issues/34)
- [ ] Arena end to end: real pair renders, preview audio plays (graceful state when a song has no preview), vote POSTs, page reloads with the next pair, round counter is accurate, finishing round 16 lands on the profile — [#34](https://github.com/Antoine-Ric/InTunes/issues/34)
- [ ] Disable vote controls while a submission is in flight; on failure preserve the issued pair and show a retry/error state. Unavailable or failed audio must never display simulated playback in live mode — [#34](https://github.com/Antoine-Ric/InTunes/issues/34)
- [ ] Community page end to end: posts render from the DB; post / reply / like / pin all work from community.js against the real endpoints — [#35](https://github.com/Antoine-Ric/InTunes/issues/35)
- [ ] Add a reply composer with validation and an actual reply submission handler; the existing reply toggle only expands/collapses replies — [#35](https://github.com/Antoine-Ric/InTunes/issues/35)
- [ ] Render current-user identity, profile traits, community names, saved like/pin state, and pagination controls from live data rather than hardcoded demo values — [#35](https://github.com/Antoine-Ric/InTunes/issues/35)
- [ ] Explore page renders real communities with member counts (derived: count TasteProfiles per archetypeKey, no extra column) — [#35](https://github.com/Antoine-Ric/InTunes/issues/35)
- [ ] Empty states: genre not seeded yet, `/profile` before any run is complete, a community with zero posts — [#36](https://github.com/Antoine-Ric/InTunes/issues/36)
- [ ] Remove automatic demo fallback on live API failures; any retained demo mode must be explicitly enabled. Failed posts/likes/pins must not look successfully saved — [#36](https://github.com/Antoine-Ric/InTunes/issues/36)
- [ ] Friendly failure surface for the domain exceptions (genre not seeded, profile not ready) instead of stack traces — [#36](https://github.com/Antoine-Ric/InTunes/issues/36)
- [ ] Complete sharing: generate `shareText` from the actual profile/top song and provide a screenshot-friendly card. Decide whether public profile links are in MVP; if included, add a user/genre-specific read-only route, define what is public, and allow it through security — [#37](https://github.com/Antoine-Ric/InTunes/issues/37)
- [ ] Verify keyboard operation, focus/error feedback, and mobile layouts for registration, arena voting/audio, and community forms — [#38](https://github.com/Antoine-Ric/InTunes/issues/38)

## Verification throughout the phases

- [x] Add a Spring application-context/startup test with PostgreSQL: `InTunesApplicationTest` uses an isolated Testcontainers database; 34 existing tests plus 2 startup/HTTP tests pass, with clean context/container shutdown — [#12](https://github.com/Antoine-Ric/InTunes/issues/12)
- [ ] Add PostgreSQL integration tests for repository queries, uniqueness/check constraints, migrations, transactions, and concurrent mutations; mocked repositories cannot verify those behaviors — [#39](https://github.com/Antoine-Ric/InTunes/issues/39)
- [ ] Add controller/security tests and live-model Thymeleaf rendering tests, including the arena DTO and profile/community model contracts — [#40](https://github.com/Antoine-Ric/InTunes/issues/40)
- [ ] Add one browser test for register → genre → 16 votes → profile → community post/reply/like/pin, plus vote retry and resume scenarios — [#41](https://github.com/Antoine-Ric/InTunes/issues/41)
- [ ] Add CI on pull requests: Java 21 build, unit/integration tests, and migration validation against a fresh PostgreSQL database; keep live-provider credential checks separate from deterministic CI tests — [#42](https://github.com/Antoine-Ric/InTunes/issues/42)

## Phase 8 — Deploy (MVP = a live URL)

- [ ] Prod config: datasource from host env vars, migrations before `ddl-auto: validate`, `show-sql` off, Spotify + Last.fm secrets via env only, and local demo identity/fallback disabled — [#43](https://github.com/Antoine-Ric/InTunes/issues/43)
- [ ] Pick the host and managed PostgreSQL offering after checking current pricing/limits; add deploy config (Dockerfile or platform settings: Java version, port from `$PORT`) — [#43](https://github.com/Antoine-Ric/InTunes/issues/43)
- [ ] Create the prod database and app; set all env vars — [#43](https://github.com/Antoine-Ric/InTunes/issues/43)
- [ ] Prove a fresh deployment creates its schema through migrations and starts successfully with production configuration — [#43](https://github.com/Antoine-Ric/InTunes/issues/43)
- [ ] Seed prod through the chosen controlled entry point; verify eligible counts, metadata, and audio before opening the genre pools — [#44](https://github.com/Antoine-Ric/InTunes/issues/44)
- [ ] Smoke test the golden path on the live URL: register → pick a genre → 16 rounds → archetype → share → community post/reply/like/pin → explore; verify unauthorized users cannot seed or mutate another user's data — [#45](https://github.com/Antoine-Ric/InTunes/issues/45)
- [x] README links to the local startup guide — [#11](https://github.com/Antoine-Ric/InTunes/issues/11)
- [ ] Keep startup instructions current with the implemented clients; commit `.env.example` placeholders and document required versus optional variables, Java tooling, and how to recreate the ignored VS Code launch configuration — [#46](https://github.com/Antoine-Ric/InTunes/issues/46)

---

**Immediate next step:** settle the genre, song-membership, preview-eligibility, and run-lifecycle decisions in Phases 1–3. Establish migrations and atomic/idempotent vote rules, then implement MatchupService with the DTO/template contract aligned. Spring/PostgreSQL startup is verified by two regression tests delivered on `main`. These foundations need design and integration checks, not just mechanical accessor changes.
