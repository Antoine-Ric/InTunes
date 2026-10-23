# Run InTunes locally

These steps use Docker for PostgreSQL and VS Code to launch the Spring Boot app.

## Requirements

- Java 21 and Maven.
- Docker Desktop.
- VS Code with the Extension Pack for Java.
- The project's `.vscode/launch.json` configuration named **Launch InTunes**.

## Start the app

1. Open Docker Desktop and wait until it is running.

2. Open the `SI-TUNES` project folder in VS Code. In the integrated terminal, go to the project root:

   ```bash
   cd /Users/ricardiantoine/Developer/SI-TUNES
   ```

3. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

   Check that it is ready:

   ```bash
   docker compose exec postgres pg_isready -U intunes -d intunes
   ```

   Wait until the output says `accepting connections`. If needed, run the check again after a few seconds.

   The Compose configuration creates the `intunes` database with username `intunes` and password `intunes` on `localhost:5432`.

4. Keep any secrets in the project's root `.env` file. For Spotify, the variable names are:

   ```dotenv
   SPOTIFY_CLIENT_ID=your_client_id
   SPOTIFY_CLIENT_SECRET=your_client_secret
   ```

   Replace the placeholders with your credentials if using Spotify. Spotify credentials are optional for starting the current scaffold; its integration is unfinished. Keep `.env` ignored by Git.

5. Open **Run and Debug** with **Shift+Command+D**, select **Launch InTunes**, and press **F5**.

   The launch configuration loads `.env` automatically. Wait for `Started InTunesApplication` in the terminal.

6. Open [http://localhost:8080](http://localhost:8080).

   The app currently includes demo pages. Several backend features are still unfinished.

## Stop the app

1. Stop the app in VS Code with **Shift+F5**.
2. Stop PostgreSQL:

   ```bash
   docker compose stop postgres
   ```

   Your database data stays saved in Docker's `intunes-pgdata` volume.

## Troubleshooting

- **Spring imports cannot be resolved:** open the Command Palette with **Shift+Command+P**, run **Java: Clean Java Language Server Workspace**, and choose **Restart and delete** when prompted. Let Java finish importing the Maven project, then launch again.
- **Cannot connect to the Docker daemon:** open Docker Desktop and wait until it is running.
- **Database connection refused:** repeat the PostgreSQL startup and readiness commands in step 3. Inspect database logs with `docker compose logs postgres` if it does not become ready.
- **Port 5432 or 8080 is already in use:** stop the other database or app using that port, then retry.

## Run the Elo service tests

From the project root:

```bash
mvn -Dtest=EloRatingServiceTest test
```
