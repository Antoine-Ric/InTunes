# InTunes MVP — GitHub issue index

Created Oct 23, 2026. [Master tracker #6](https://github.com/Antoine-Ric/InTunes/issues/6) · [MVP milestone](https://github.com/Antoine-Ric/InTunes/milestone/1) · [Source checklist](intunes-mvp-checklist.md).

All 109 source checklist items map exactly once to 40 delivery issues. A master delivery tracker and completed planning record bring the total to 42 issues. This status snapshot includes the startup fix delivered on `main` for #12; check GitHub issue labels for current progress.

## Workflow

| Title bracket | Status label | Meaning | GitHub state |
|---|---|---|---|
| `[BACKLOG]` | `status: backlog` | Planned; implementation has not started | Open |
| `[IN PROGRESS]` | `status: in-progress` | Active work or overall delivery tracker | Open |
| `[DONE]` | `status: done` | Verified non-code work, code delivered directly to main, or completed code awaiting merge | Closed when delivered; open while awaiting merge |
| `[MERGED]` | `status: merged` | Delivered in a PR merged into main | Closed as completed |

Keep one status label and update the title bracket manually when work changes state. Use `Closes #<issue>` in a delivery PR; after merge, record the PR link and update the closed issue to `[MERGED]`. For an explicitly requested direct push to main, close the delivered issue as `[DONE]` with its commit link. Phase labels group work. P0 identifies foundation/correctness/access-control work, P1 required MVP work, and P2 supporting documentation/polish or historical delivery.

GitHub Projects access was unavailable with the current credentials. The milestone, linked master task list, and label filters provide the backlog without additional permissions.

## Status views

- [Backlog](https://github.com/Antoine-Ric/InTunes/issues?q=is%3Aissue%20label%3A%22status%3A%20backlog%22)
- [In Progress](https://github.com/Antoine-Ric/InTunes/issues?q=is%3Aissue%20label%3A%22status%3A%20in-progress%22)
- [Done](https://github.com/Antoine-Ric/InTunes/issues?q=is%3Aissue%20label%3A%22status%3A%20done%22)
- [Merged](https://github.com/Antoine-Ric/InTunes/issues?q=is%3Aissue%20label%3A%22status%3A%20merged%22)

## Status snapshot

- `[IN PROGRESS]` [#6 — MVP master delivery tracker](https://github.com/Antoine-Ric/InTunes/issues/6); no unfinished implementation task was marked active without evidence.
- `[DONE]` [#47 — MVP plan review](https://github.com/Antoine-Ric/InTunes/issues/47); the revised checklist and issue index are delivered on `main`.
- `[DONE]` [#12 — Spring startup regression fix](https://github.com/Antoine-Ric/InTunes/issues/12); 36 tests pass, including PostgreSQL startup and HTTP checks. Delivered directly on `main` and closed as completed.
- `[MERGED]`: 5 closed historical delivery issues with PR #5 evidence.
- `[BACKLOG]`: 34 open delivery issues.

## Recommended starting order

1. Completed prerequisite: [#12 — Spring startup fix](https://github.com/Antoine-Ric/InTunes/issues/12), delivered on `main`.
2. [#13 — Genres, membership, and run lifecycle](https://github.com/Antoine-Ric/InTunes/issues/13), then [#20 — Preview eligibility](https://github.com/Antoine-Ric/InTunes/issues/20).
3. [#14 — Entity APIs](https://github.com/Antoine-Ric/InTunes/issues/14).
4. [#15 — Migrations and integrity](https://github.com/Antoine-Ric/InTunes/issues/15), then [#16 — Repository queries](https://github.com/Antoine-Ric/InTunes/issues/16).
5. [#22 — Pairing](https://github.com/Antoine-Ric/InTunes/issues/22) → [#23 — Issued pairs and resume](https://github.com/Antoine-Ric/InTunes/issues/23) → [#24 — Safe votes](https://github.com/Antoine-Ric/InTunes/issues/24) → [#25 — Arena wiring](https://github.com/Antoine-Ric/InTunes/issues/25).

## Merged foundations

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [7 — [MERGED] Establish the Spring Boot, PostgreSQL, and entity scaffold](https://github.com/Antoine-Ric/InTunes/issues/7) | P2 | — | 1, 2 |
| [8 — [MERGED] Deliver the Thymeleaf interface and browser demo interactions](https://github.com/Antoine-Ric/InTunes/issues/8) | P2 | — | 3, 4 |
| [9 — [MERGED] Implement Elo math and establish the 34-test unit baseline](https://github.com/Antoine-Ric/InTunes/issues/9) | P2 | — | 5, 8 |
| [10 — [MERGED] Implement music provider clients and the song seeding service](https://github.com/Antoine-Ric/InTunes/issues/10) | P2 | — | 6, 7 |
| [11 — [MERGED] Publish the local startup guide and README entry point](https://github.com/Antoine-Ric/InTunes/issues/11) | P2 | — | 9, 108 |

## Phase 1 — Persistence foundations

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [13 — [BACKLOG] Finalize genres, song membership, and run lifecycle](https://github.com/Antoine-Ric/InTunes/issues/13) | P0 | — | 24, 25, 26 |
| [14 — [BACKLOG] Complete entity constructors and application-facing accessors](https://github.com/Antoine-Ric/InTunes/issues/14) | P0 | — | 12 |
| [15 — [BACKLOG] Introduce migrations, integrity constraints, and concurrency rules](https://github.com/Antoine-Ric/InTunes/issues/15) | P0 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13) | 13, 14, 15 |
| [16 — [BACKLOG] Implement repository queries for matchups, profiles, and boards](https://github.com/Antoine-Ric/InTunes/issues/16) | P0 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13), [#14](https://github.com/Antoine-Ric/InTunes/issues/14), [#15](https://github.com/Antoine-Ric/InTunes/issues/15) | 16, 17, 18, 19, 20, 21, 22, 23 |

## Phase 2 — Seeding goes live

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [17 — [BACKLOG] Verify live Spotify, iTunes, and Last.fm access](https://github.com/Antoine-Ric/InTunes/issues/17) | P1 | [#12](https://github.com/Antoine-Ric/InTunes/issues/12) | 11 |
| [18 — [BACKLOG] Add a controlled seeding entry point and admin access rules](https://github.com/Antoine-Ric/InTunes/issues/18) | P1 | [#15](https://github.com/Antoine-Ric/InTunes/issues/15) | 27, 28 |
| [19 — [BACKLOG] Harden provider timeouts, retries, and preview identity](https://github.com/Antoine-Ric/InTunes/issues/19) | P1 | — | 30, 31 |
| [20 — [BACKLOG] Define preview eligibility consistently across the app](https://github.com/Antoine-Ric/InTunes/issues/20) | P0 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13) | 32 |
| [21 — [BACKLOG] Curate, commit, seed, and verify every local genre pool](https://github.com/Antoine-Ric/InTunes/issues/21) | P1 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13), [#20](https://github.com/Antoine-Ric/InTunes/issues/20), [#17](https://github.com/Antoine-Ric/InTunes/issues/17), [#18](https://github.com/Antoine-Ric/InTunes/issues/18), [#19](https://github.com/Antoine-Ric/InTunes/issues/19) | 29, 33, 34, 35 |

## Phase 3 — MatchupService

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [22 — [BACKLOG] Implement fair pairing and run completion rules](https://github.com/Antoine-Ric/InTunes/issues/22) | P0 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13), [#20](https://github.com/Antoine-Ric/InTunes/issues/20), [#16](https://github.com/Antoine-Ric/InTunes/issues/16) | 36, 37, 43, 44, 45 |
| [23 — [BACKLOG] Persist issued pairs, resume state, and skip behavior](https://github.com/Antoine-Ric/InTunes/issues/23) | P0 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13), [#20](https://github.com/Antoine-Ric/InTunes/issues/20), [#22](https://github.com/Antoine-Ric/InTunes/issues/22) | 38, 42 |
| [24 — [BACKLOG] Record atomic, validated, idempotent Elo votes](https://github.com/Antoine-Ric/InTunes/issues/24) | P0 | [#15](https://github.com/Antoine-Ric/InTunes/issues/15), [#16](https://github.com/Antoine-Ric/InTunes/issues/16), [#22](https://github.com/Antoine-Ric/InTunes/issues/22), [#23](https://github.com/Antoine-Ric/InTunes/issues/23) | 39, 40, 41, 46 |
| [25 — [BACKLOG] Wire matchup controllers, DTOs, and genre-aware redirects](https://github.com/Antoine-Ric/InTunes/issues/25) | P1 | [#24](https://github.com/Antoine-Ric/InTunes/issues/24) | 47, 48, 49, 50 |

## Phase 4 — TasteProfileService

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [26 — [BACKLOG] Define and calculate top-five archetype buckets](https://github.com/Antoine-Ric/InTunes/issues/26) | P1 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13), [#20](https://github.com/Antoine-Ric/InTunes/issues/20), [#16](https://github.com/Antoine-Ric/InTunes/issues/16), [#22](https://github.com/Antoine-Ric/InTunes/issues/22) | 51, 52, 53, 54, 55 |
| [27 — [BACKLOG] Author the complete archetype naming matrix](https://github.com/Antoine-Ric/InTunes/issues/27) | P1 | [#13](https://github.com/Antoine-Ric/InTunes/issues/13) | 56 |
| [28 — [BACKLOG] Persist profiles idempotently and wire ranked profile data](https://github.com/Antoine-Ric/InTunes/issues/28) | P1 | [#24](https://github.com/Antoine-Ric/InTunes/issues/24), [#26](https://github.com/Antoine-Ric/InTunes/issues/26), [#27](https://github.com/Antoine-Ric/InTunes/issues/27) | 57, 58, 59, 60, 61 |

## Phase 5 — CommunityService

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [29 — [BACKLOG] Resolve communities and implement paginated board reads](https://github.com/Antoine-Ric/InTunes/issues/29) | P1 | [#15](https://github.com/Antoine-Ric/InTunes/issues/15), [#16](https://github.com/Antoine-Ric/InTunes/issues/16), [#27](https://github.com/Antoine-Ric/InTunes/issues/27) | 62, 63, 64, 72 |
| [30 — [BACKLOG] Implement validated community posts, replies, and resource access](https://github.com/Antoine-Ric/InTunes/issues/30) | P1 | [#29](https://github.com/Antoine-Ric/InTunes/issues/29) | 65, 66, 70, 71 |
| [31 — [BACKLOG] Implement consistent likes, pin ownership, and concurrency tests](https://github.com/Antoine-Ric/InTunes/issues/31) | P1 | [#15](https://github.com/Antoine-Ric/InTunes/issues/15), [#30](https://github.com/Antoine-Ric/InTunes/issues/30) | 67, 68, 69, 73, 74, 75 |

## Phase 6 — Real auth

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [32 — [BACKLOG] Implement registration, session login, and real user resolution](https://github.com/Antoine-Ric/InTunes/issues/32) | P0 | [#14](https://github.com/Antoine-Ric/InTunes/issues/14), [#16](https://github.com/Antoine-Ric/InTunes/issues/16) | 76, 77, 78, 79, 81, 82, 83 |
| [33 — [BACKLOG] Enable CSRF tokens and test authentication/authorization boundaries](https://github.com/Antoine-Ric/InTunes/issues/33) | P0 | [#32](https://github.com/Antoine-Ric/InTunes/issues/32), [#18](https://github.com/Antoine-Ric/InTunes/issues/18), [#25](https://github.com/Antoine-Ric/InTunes/issues/25), [#31](https://github.com/Antoine-Ric/InTunes/issues/31) | 80, 84 |

## Phase 7 — UI wiring and polish

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [34 — [BACKLOG] Connect the genre picker and reliable live arena](https://github.com/Antoine-Ric/InTunes/issues/34) | P1 | [#21](https://github.com/Antoine-Ric/InTunes/issues/21), [#25](https://github.com/Antoine-Ric/InTunes/issues/25), [#28](https://github.com/Antoine-Ric/InTunes/issues/28), [#32](https://github.com/Antoine-Ric/InTunes/issues/32) | 85, 86, 87 |
| [35 — [BACKLOG] Wire board interactions, reply composer, and live identity/counts](https://github.com/Antoine-Ric/InTunes/issues/35) | P1 | [#28](https://github.com/Antoine-Ric/InTunes/issues/28), [#29](https://github.com/Antoine-Ric/InTunes/issues/29), [#30](https://github.com/Antoine-Ric/InTunes/issues/30), [#31](https://github.com/Antoine-Ric/InTunes/issues/31), [#32](https://github.com/Antoine-Ric/InTunes/issues/32) | 88, 89, 90, 91 |
| [36 — [BACKLOG] Replace silent demo fallbacks with useful live error states](https://github.com/Antoine-Ric/InTunes/issues/36) | P1 | [#25](https://github.com/Antoine-Ric/InTunes/issues/25), [#30](https://github.com/Antoine-Ric/InTunes/issues/30), [#32](https://github.com/Antoine-Ric/InTunes/issues/32) | 92, 93, 94 |
| [37 — [BACKLOG] Complete profile sharing and decide public profile links](https://github.com/Antoine-Ric/InTunes/issues/37) | P1 | [#28](https://github.com/Antoine-Ric/InTunes/issues/28), [#31](https://github.com/Antoine-Ric/InTunes/issues/31), [#32](https://github.com/Antoine-Ric/InTunes/issues/32) | 95 |
| [38 — [BACKLOG] Verify mobile, keyboard, focus, and accessible feedback](https://github.com/Antoine-Ric/InTunes/issues/38) | P2 | [#34](https://github.com/Antoine-Ric/InTunes/issues/34), [#35](https://github.com/Antoine-Ric/InTunes/issues/35), [#36](https://github.com/Antoine-Ric/InTunes/issues/36) | 96 |

## Verification throughout the phases

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [12 — [DONE] Verify Spring startup with PostgreSQL and add a context test](https://github.com/Antoine-Ric/InTunes/issues/12) | P0 | — | 10, 97 |
| [39 — [BACKLOG] Add PostgreSQL migration, repository, and concurrency integration tests](https://github.com/Antoine-Ric/InTunes/issues/39) | P0 | [#15](https://github.com/Antoine-Ric/InTunes/issues/15), [#16](https://github.com/Antoine-Ric/InTunes/issues/16), [#24](https://github.com/Antoine-Ric/InTunes/issues/24), [#31](https://github.com/Antoine-Ric/InTunes/issues/31) | 98 |
| [40 — [BACKLOG] Test controller security and live Thymeleaf model contracts](https://github.com/Antoine-Ric/InTunes/issues/40) | P1 | [#25](https://github.com/Antoine-Ric/InTunes/issues/25), [#28](https://github.com/Antoine-Ric/InTunes/issues/28), [#31](https://github.com/Antoine-Ric/InTunes/issues/31), [#33](https://github.com/Antoine-Ric/InTunes/issues/33) | 99 |
| [41 — [BACKLOG] Automate the browser golden path, retries, and resume](https://github.com/Antoine-Ric/InTunes/issues/41) | P1 | [#34](https://github.com/Antoine-Ric/InTunes/issues/34), [#35](https://github.com/Antoine-Ric/InTunes/issues/35), [#36](https://github.com/Antoine-Ric/InTunes/issues/36), [#33](https://github.com/Antoine-Ric/InTunes/issues/33) | 100 |
| [42 — [BACKLOG] Run Java 21 tests and fresh-database validation in CI](https://github.com/Antoine-Ric/InTunes/issues/42) | P1 | [#12](https://github.com/Antoine-Ric/InTunes/issues/12), [#15](https://github.com/Antoine-Ric/InTunes/issues/15) | 101 |

## Phase 8 — Deploy

| Issue | Priority | Depends on | Source items |
|---|---|---|---|
| [43 — [BACKLOG] Configure hosting, production database, and fresh deployment](https://github.com/Antoine-Ric/InTunes/issues/43) | P1 | [#15](https://github.com/Antoine-Ric/InTunes/issues/15), [#33](https://github.com/Antoine-Ric/InTunes/issues/33), [#39](https://github.com/Antoine-Ric/InTunes/issues/39), [#40](https://github.com/Antoine-Ric/InTunes/issues/40), [#42](https://github.com/Antoine-Ric/InTunes/issues/42) | 102, 103, 104, 105 |
| [44 — [BACKLOG] Seed production and verify launch-ready genre pools](https://github.com/Antoine-Ric/InTunes/issues/44) | P1 | [#43](https://github.com/Antoine-Ric/InTunes/issues/43), [#21](https://github.com/Antoine-Ric/InTunes/issues/21) | 106 |
| [45 — [BACKLOG] Smoke-test the public MVP journey and access boundaries](https://github.com/Antoine-Ric/InTunes/issues/45) | P1 | [#44](https://github.com/Antoine-Ric/InTunes/issues/44), [#37](https://github.com/Antoine-Ric/InTunes/issues/37), [#38](https://github.com/Antoine-Ric/InTunes/issues/38), [#41](https://github.com/Antoine-Ric/InTunes/issues/41) | 107 |
| [46 — [BACKLOG] Update startup docs and add a safe environment example](https://github.com/Antoine-Ric/InTunes/issues/46) | P2 | [#18](https://github.com/Antoine-Ric/InTunes/issues/18), [#32](https://github.com/Antoine-Ric/InTunes/issues/32) | 109 |
