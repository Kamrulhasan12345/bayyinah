# Bayyinah

Bayyinah is a multi-module Quran learning platform with a JavaFX desktop client, a Spring Boot backend, and a TypeScript data generation pipeline.

## Project layout

| Path               | Purpose                                                  |
| ------------------ | -------------------------------------------------------- |
| `bayyinah-core/`   | Shared domain models and query contracts                 |
| `bayyinah-client/` | JavaFX desktop app (offline-first + realtime meeting UI) |
| `bayyinah-server/` | Spring Boot API, auth, and STOMP signaling for meetings  |
| `data/`            | Generated SQLite database and audio files                |
| `scripts/`         | TypeScript pipeline that builds data artifacts           |

## Prerequisites

- Java 21
- Maven 3.8+
- Node.js 22+ (required by `node:sqlite` in scripts)
- PostgreSQL (for server runtime)

## Quick start

### 1) Generate data artifacts

From `scripts/`:

```bash
# copy .env.example to .env (PowerShell: Copy-Item .env.example .env)
npm install
node --env-file=.env --import=tsx src/index.ts
```

This creates `data/quran.db` and optional audio files under `data/audio/`.

### 2) Point the desktop client to generated data

On first start, the client creates `~/.bayyinah/config.yaml` with defaults.

Update at least these fields so they point to your generated assets:

```yaml
quran:
  databasePath: /absolute/path/to/bayyinah/data/quran.db
audio:
  audioRootPath: /absolute/path/to/bayyinah/data/audio
```

### 3) Build everything

From repository root:

```bash
mvn clean install
```

### 4) Run modules

Desktop client from root:

```bash
mvn -pl bayyinah-client javafx:run
```

Server (PowerShell on Windows):

```powershell
cd bayyinah-server
.\mvnw.cmd spring-boot:run
```

Server (bash):

```bash
cd bayyinah-server
./mvnw spring-boot:run
```

## Architecture flow

1. `scripts` fetches Quran content and writes SQLite/audio artifacts.
2. `data` stores the generated assets.
3. `bayyinah-core` defines shared domain/query contracts.
4. `bayyinah-client` reads local data and can sync with server services.
5. `bayyinah-server` provides auth, user sync APIs, and realtime room signaling.

## Documentation map

- [Core module guide](bayyinah-core/readme.md)
- [Client module guide](bayyinah-client/readme.md)
- [Server module guide](bayyinah-server/readme.md)
- [Data directory guide](data/readme.md)
- [Scripts guide](scripts/readme.md)
- [Collaborative meeting design docs](bayyinah-server/docs/collaborative-meeting/README.md)

## Notes

- Root build includes all Maven modules listed in `pom.xml`.
- Server reads environment values via `.env` (local) or `.env.prod` (docker compose setup).
- Current test coverage is limited; run `mvn test` but expect mostly baseline tests.
