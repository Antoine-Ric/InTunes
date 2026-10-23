# Docker Desktop and SQL reference

Use this guide to start InTunes' local PostgreSQL database, inspect its tables,
and check database upgrades. For launching the application, see
[Run InTunes locally](RUN_LOCALLY.md).

## What each piece does

- **Docker Desktop** runs and manages the database container.
- **PostgreSQL** is the database software inside that container.
- **`intunes`** is the database holding the app's tables.
- **Tables** are like spreadsheets: columns describe the fields, and rows hold
  individual songs, users, votes, and other records.
- **SQL** is the language used to ask the database for information.
- **Flyway** keeps a log of database structure changes and applies new upgrades
  when the application starts.

## Local connection details

These development defaults come from [docker-compose.yml](../docker-compose.yml).

| Setting | Value |
|---|---|
| Container name | `intunes-postgres` |
| Compose service | `postgres` |
| PostgreSQL version | 16 |
| Host, when connecting from your Mac | `localhost` |
| Port | `5432` |
| Database | `intunes` |
| Username | `intunes` |
| Local development password | `intunes` |
| Compose data volume | `intunes-pgdata` |

Docker Desktop may display the volume name with a project prefix. The volume
stores the database separately from the running container, so stopping and
starting the container preserves its data.

The application uses these defaults unless `DATABASE_URL`, `DATABASE_USERNAME`,
or `DATABASE_PASSWORD` overrides them. See [persistence configuration](PERSISTENCE.md).

## Start and inspect the container

1. Open Docker Desktop and wait until its engine is running.
2. Select **Containers**.
3. Expand the InTunes/SI-TUNES project group if necessary, or search for
   **intunes-postgres**.
4. If the container is stopped, select its **Start** button.
5. Select the container to open its details.

The **Logs** tab shows PostgreSQL's startup messages and errors. A message
containing `database system is ready to accept connections` means it is ready.
To see tables and rows, open the database console as described below.

If the container has not been created yet, run this from the project root in
the VS Code terminal:

```bash
docker compose up -d postgres
```

Useful commands in the **VS Code or Mac terminal**:

```bash
# Show the project's containers and their status.
docker compose ps

# Check whether PostgreSQL is accepting connections.
docker compose exec -T postgres pg_isready -U intunes -d intunes

# Show recent database logs.
docker compose logs --tail=50 postgres

# Stop the database when finished; the volume keeps its data.
docker compose stop postgres
```

## Open the database console in Docker Desktop

1. Select the running **intunes-postgres** container.
2. Open **Exec**. If Docker Debug is enabled, the tab may instead say **Debug**.
3. Enter:

   ```bash
   psql -U intunes -d intunes
   ```

`psql` opens the PostgreSQL console. `-U intunes` chooses the user, and
`-d intunes` chooses the database. The prompt should become `intunes=#`.

Alternatively, open the same console directly from the project root in your
**VS Code or Mac terminal**:

```bash
docker compose exec postgres psql -U intunes -d intunes
```

Run the commands in the following sections at the **`intunes=#` prompt**.
SQL statements such as `SELECT` end with a semicolon. Console shortcuts starting
with `\`, such as `\dt`, do not need one.

## Console shortcuts

| Command | What it does |
|---|---|
| `\conninfo` | Confirms the database and user you connected as. |
| `\dt` | Lists the tables. |
| `\d songs` | Shows the columns, indexes, and rules for the `songs` table. |
| `\d matchup_runs` | Shows the structure of the run-history table. |
| `\pset pager off` | Prints long results directly instead of opening a scrolling viewer. |
| `\q` | Leaves the database console. |

If a result opens a viewer showing `(END)`, press **q** to return to the prompt.
If an unfinished SQL statement leaves the prompt as `intunes-#`, finish it with
`;` or press **Ctrl+C** to cancel that statement.

## SQL commands to keep handy

### Count songs

```sql
SELECT COUNT(*) AS song_count FROM songs;
```

### View up to 10 songs

```sql
SELECT id, title, artist, genre, release_year, popularity
FROM songs
ORDER BY id
LIMIT 10;
```

### Count songs in each genre

```sql
SELECT genre, COUNT(*) AS song_count
FROM songs
GROUP BY genre
ORDER BY genre;
```

### View users' public fields

```sql
SELECT id, username, display_name, created_at
FROM users
ORDER BY id
LIMIT 10;
```

### View recent attempts through a genre

A run is one user's attempt through one genre. A retake creates another run.

```sql
SELECT id, user_id, genre, status, started_at, finished_at
FROM matchup_runs
ORDER BY started_at DESC, id DESC
LIMIT 10;
```

### Count records across all application tables

```sql
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'songs', COUNT(*) FROM songs
UNION ALL SELECT 'matchup_runs', COUNT(*) FROM matchup_runs
UNION ALL SELECT 'song_ratings', COUNT(*) FROM song_ratings
UNION ALL SELECT 'matchups', COUNT(*) FROM matchups
UNION ALL SELECT 'taste_profiles', COUNT(*) FROM taste_profiles
UNION ALL SELECT 'communities', COUNT(*) FROM communities
UNION ALL SELECT 'posts', COUNT(*) FROM posts
UNION ALL SELECT 'replies', COUNT(*) FROM replies
UNION ALL SELECT 'likes', COUNT(*) FROM likes
ORDER BY table_name;
```

`0` or `(0 rows)` means there are no matching records. The application can show
demo content even when these tables are empty; displayed demo songs are not
automatically saved to the database.

## Check database upgrades

```sql
SELECT version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

After upgrading the original local database, the initial history is:

| Version | Description | Type | Success |
|---|---|---|---|
| 1 | `<< Flyway Baseline >>` | `BASELINE` | `t` |
| 2 | `runs integrity and indexes` | `SQL` | `t` |

`t` means true: that step succeeded. A fresh database records version 1 as a
`SQL` migration named `scaffold baseline` instead. Future migrations add more
entries, so additional rows are expected as the project develops.

On October 23, 2026, the existing local database was backed up, registered as
version 1, and upgraded to version 2. Normal startup was then verified with an
HTTP 200 response. The local backup from that upgrade is
`.local/backups/intunes-before-flyway.jgRM1u`, relative to the project root;
it is a local artifact and is not included in Git.

If another existing database reports **non-empty schema but no schema history
table**, follow the [one-time baseline procedure](PERSISTENCE.md#existing-local-scaffold-databases).
The database already upgraded above uses normal startup.

## References

- [Docker Desktop container details and Exec/Debug](https://docs.docker.com/desktop/use-desktop/container/)
- [PostgreSQL 16 psql reference](https://www.postgresql.org/docs/16/app-psql.html)
- [InTunes migration and concurrency rules](PERSISTENCE.md)
