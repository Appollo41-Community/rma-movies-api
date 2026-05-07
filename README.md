# RMA Movies API

Backend for the RAF Mobile-Application-Development course projects:

- **`/movies`, `/people`, `/genres`, `/collections`, `/config`** — movie catalogue (Project 1, Premiere)
- **`/beskar/*`** — astronomy teaching API (auxiliary)
- **`/auth`, `/me`, `/leaderboard`** — Showtime users module (Project 2)

Built with Kotlin 2.2.0, Ktor 3.2.0, Koin, and Exposed over SQLite.

## Running the server

The Showtime users module requires a `JWT_SECRET` environment variable. The server fails loud at boot if it is missing.

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
./gradlew run
```

A `users.db` SQLite file is created automatically at `data/users.db` on first launch. Override the location with `USERS_DATABASE_URL`:

```bash
USERS_DATABASE_URL=/tmp/showtime-users.db JWT_SECRET=secret ./gradlew run
```

The movies catalogue is read from `data/movies.db` (override with `DATABASE_URL`).

## Running tests

```bash
JWT_SECRET=test-secret ./gradlew test
```

## API documentation

Once the server is running, browse:

- `/docs` — index
- `/movies/docs` — movies + Showtime endpoints
- `/beskar/docs` — Beskar endpoints

## Specs and plans

Project specs and implementation plans live under `docs/superpowers/`.
