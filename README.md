**English** | [Magyar](README.hu.md)

# score-service

Score and leaderboard service for the Mini Arcade Portal. Validates and
stores game scores and serves per-difficulty leaderboards.

## Stack

- Java 21, Spring Boot 4
- Spring Security + JJWT (validates JWTs issued by auth-service)
- Spring Data JPA + PostgreSQL
- springdoc-openapi (Swagger UI)
- Testcontainers for integration tests

## Endpoints

- `POST /api/scores/sessions` — start a play session (anti-cheat token)
- `POST /api/scores` — submit a score
- `GET /api/scores` — leaderboard
- `GET /api/scores/me` — the current user's scores

Full API docs: `http://localhost:8082/swagger-ui.html`

## Running locally

The service needs PostgreSQL and a shared `JWT_SECRET`, so it's easiest to run
the whole stack from [`infra`](../infra):

```bash
cd ../infra
docker compose up --build
```

score-service is then available at `http://localhost:8082`.

Alternatively, run just this service against your own PostgreSQL instance:

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

Integration tests spin up PostgreSQL via Testcontainers — no local database
needed.
