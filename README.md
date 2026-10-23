# InTunes — MVP scaffold

Music taste profiling through head-to-head song matchups. Vote in this-vs-that
rounds, get a named music profile, land in a community of people who hear
music like you do.

## Run it

Follow the [local startup guide](docs/RUN_LOCALLY.md) for prerequisites,
PostgreSQL setup, loading `.env` secrets with VS Code, launching the app,
and stopping it when finished.

Use the [Docker Desktop and SQL reference](docs/DOCKER_DESKTOP_AND_SQL.md) to
inspect the local database, browse records, and check migration history.

## Data sources (important)

- **Spotify Web API** (client-credentials): track metadata, album art, popularity,
  release dates. Free, no user login needed for MVP.
- **iTunes Search API**: 30-second previews. Free, no key.
- Spotify **killed** audio-features, audio-analysis, recommendations, and
  `preview_url` for new apps on Nov 27, 2024. Do not build on them.
  Archetypes come from **popularity × era** instead (see TasteProfileService).

## Project map

| Package | What's there |
|---|---|
| `domain` | 11 entities: User, Song, MatchupRun, IssuedMatchupPair, Matchup, SongRating, TasteProfile, Community, Post, Reply, Like |
| `repository` | Spring Data queries for runs, issued pairs, ratings, profiles, and boards; locking and idempotent insert primitives |
| `service` | Stubs with signatures + TODOs: EloRatingService, MatchupService, TasteProfileService, CommunityService, SpotifyClient, ITunesClient |
| `web` | PageController (Thymeleaf routes), MatchupController + CommunityController (JSON APIs, stubbed) |
| `dto` | SongDto, MatchupPair records |
| `templates` | 6 pages: index, genres, matchup, profile, community, explore |
| `config` | SpotifyConfig, SecurityConfig (permissive for now — TODO: lock down) |
| `db/migration` | Flyway schema creation, run-scoped integrity constraints, and indexes; see [persistence rules](docs/PERSISTENCE.md) |

## Build order (your bodies to write)

1. `EloRatingService.update()` — pure math, great first win
2. `ITunesClient.fetchPreviewUrl()` + `SpotifyClient` token/search — the seeding pipeline
3. `MatchupService` — pair picking + vote recording
4. `TasteProfileService` — archetype matrix + naming
5. `CommunityService` — board logic
6. Wire the controllers, then real auth in SecurityConfig

## MVP scope

IN: auth (simple), 5 seeded genres × 25 songs, 16-round matchups, Elo profiles,
archetype naming, community auto-join, board (posts/replies/likes/pins),
explore page, shareable profile card.
OUT (v2): Spotify OAuth + playlist import, realtime, ML matching, DMs,
notifications, moderation dashboard, mobile app.
