# bayyinah-server

Spring Boot backend for Bayyinah authentication, user sync APIs, and collaborative meeting signaling.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC + Security + WebSocket/STOMP
- Spring Data JPA + Flyway
- PostgreSQL
- JWT authentication (access and refresh tokens)

## Configuration

### Local `.env`

Create `bayyinah-server/.env` (or copy from `.env.example`) with:

```properties
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/bayyinah
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=<base64-secret>
```

The app loads it via `spring.config.import=optional:file:.env[.properties]` from `src/main/resources/application.properties`.

### Docker `.env.prod`

`docker-compose.yml` expects `./.env.prod` and mounts it into the container runtime.

## Run

### Local (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

### Local (bash)

```bash
./mvnw spring-boot:run
```

### Docker

```bash
docker compose up --build
```

Health endpoint:

- `GET /health`

## API surface

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/refresh`

### User and preferences

- `GET /api/users/me`
- `PUT /api/users/me`
- `DELETE /api/users/me`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`

### Bookmarks

- `GET /api/bookmarks`
- `GET /api/bookmarks/{id}`
- `GET /api/bookmarks/chapters/{number}`
- `POST /api/bookmarks`
- `DELETE /api/bookmarks/{id}`

### Reading progress

- `GET /api/progress`
- `GET /api/progress/chapters/{number}`
- `GET /api/progress/current`
- `POST /api/progress`
- `DELETE /api/progress/{id}`

### Halaqah room lifecycle

- `POST /api/halaqah/create`
- `POST /api/halaqah/join`
- `POST /api/halaqah/leave`
- `GET /api/halaqah/{code}`

## STOMP routes

Client send routes:

- `/app/room/{roomId}/presence`
- `/app/room/{roomId}/offer`
- `/app/room/{roomId}/answer`
- `/app/room/{roomId}/candidate`
- `/app/room/{roomId}/chat`
- `/app/room/{roomId}/control`

Broadcast topics:

- `/topic/room/{roomId}/presence`
- `/topic/room/{roomId}/offer`
- `/topic/room/{roomId}/answer`
- `/topic/room/{roomId}/candidate`
- `/topic/room/{roomId}/chat`
- `/topic/room/{roomId}/control`

## Database and migrations

- Flyway migrations live in `src/main/resources/db/migration/`.
- Current baseline migration: `V1_initial_schema.sql`.

## Build and test

```bash
./mvnw clean package
./mvnw test
```

## Detailed design docs

- [Collaborative meeting docs index](docs/collaborative-meeting/README.md)
- [System architecture](docs/collaborative-meeting/01-system-architecture.md)
- [Runtime sequence](docs/collaborative-meeting/02-runtime-sequence.md)
- [Room state machine](docs/collaborative-meeting/03-room-state-machine.md)
- [Validation and security gates](docs/collaborative-meeting/04-validation-and-security-gates.md)
- [WebRTC signaling contract](docs/collaborative-meeting/05-webrtc-signaling-contract.md)
- [JavaFX client integration research](docs/collaborative-meeting/06-javafx-webrtc-stomp-research.md)

## Related docs

- [Repository root guide](../readme.md)
- [Client module guide](../bayyinah-client/readme.md)