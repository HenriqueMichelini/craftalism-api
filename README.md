# Craftalism API

> Core backend service that manages players, balances, and transactions for the Craftalism economy through a JWT-secured REST API.

---

## Overview

The Craftalism API is the central data service for the economy platform. It exposes REST endpoints consumed by both the Craftalism Dashboard and the Minecraft plugin. All write operations require a valid JWT issued by the Craftalism Authorization Server.

**Key capabilities:**

- Player registration and lookup by UUID or display name.
- Balance lifecycle management: create, deposit, withdraw, set, and rank.
- Transaction record storage between players.
- JWT scope-based authorization for all endpoints.
- Standardized RFC 9457 `ProblemDetail` error responses.
- Interactive API documentation via Swagger UI (local profile).

> **Important:** `POST /api/transactions` stores a transaction record only. It does **not** transfer balances. Use `POST /api/balances/transfer` for atomic balance movement. Recording a transaction and moving balances are still separate operations.

---

## Architecture

The codebase follows a classic layered architecture. Each layer has a single responsibility and communicates only with the layer directly below it.

### Controller layer (`controller/`)

Exposes REST endpoints under `/api/**`. Validates request DTOs using Bean Validation and returns typed DTO responses with appropriate HTTP status codes.

### Service layer (`service/`)

Encapsulates business rules and transactional behavior. Throws typed domain exceptions for constraint violations (e.g., insufficient funds, duplicate player).

### Repository layer (`repository/`)

Spring Data JPA repositories with custom queries for top-balance ranking and pessimistic locking on concurrent balance updates.

### Domain/model layer (`model/`)

JPA entities: `Player`, `Balance`, `Transaction`.

### Cross-cutting concerns

- `config/` — Security configuration (resource server, scope rules) and OpenAPI configuration.
- `exceptions/` — Centralized exception handler mapping domain exceptions to `ProblemDetail` responses.
- `mapper/` — Entity-to-DTO mapping components.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Web | Spring Web |
| Persistence | Spring Data JPA |
| Validation | Spring Validation |
| Security | Spring Security + OAuth2 Resource Server (JWT) |
| Database (Docker profile) | PostgreSQL |
| Database (local profile) | H2 in-memory |
| Migrations | Flyway (Docker profile only) |
| API Docs | springdoc-openapi (Swagger UI) |
| Build Tool | Gradle |
| Testing | JUnit 5, Mockito, Spring Test |

---

## Prerequisites

- Java 17+
- Docker Engine 20.10+ and Docker Compose v2+ *(for containerized deployment only)*

---

## Configuration

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — | **Required.** Set to `local` for H2 or `docker` for PostgreSQL. |
| `AUTH_ISSUER_URI` | `http://localhost:9000` (local) / `http://craftalism-auth-server:9000` (docker) | JWT issuer URI. Must match the Authorization Server's configured issuer. |
| `SPRING_DATASOURCE_URL` | — | JDBC connection string *(Docker profile only)*. |
| `SPRING_DATASOURCE_USERNAME` | — | Database username *(Docker profile only)*. |
| `SPRING_DATASOURCE_PASSWORD` | — | Database password *(Docker profile only)*. |

Additional settings are managed per profile:

- `src/main/resources/application.properties` — shared defaults.
- `src/main/resources/application-local.properties` — H2, create-drop schema, Flyway disabled.
- `src/main/resources/application-docker.properties` — PostgreSQL, Flyway enabled.

### Security model

The API is a stateless OAuth2 resource server. JWTs are validated against the issuer URI.

| Scope | Permitted methods |
|---|---|
| *(no scope required)* | `GET /api/**` (public read policy for current MVP) |
| `SCOPE_api:write` | `POST`, `PUT`, `PATCH`, `DELETE` on `/api/**` |

Public paths (no token required): `GET /actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.

### Error contract

All errors are returned as RFC 9457 `ProblemDetail` with these additional fields:

| Field | Description |
|---|---|
| `type` | One of: `.../validation`, `.../business-rule`, `.../internal` |
| `detail` | Human-readable error description. |
| `status` | HTTP status code. |
| `timestamp` | ISO 8601 timestamp. |
| `path` | Request path. |
| `errors` | Field-level validation map (validation errors only). |

---

## Running Locally

Runs with H2 in-memory database. Schema is recreated on each startup. Flyway is disabled.

```bash
cd java
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

| Endpoint | URL |
|---|---|
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/api-docs` |
| H2 Console | `http://localhost:8080/h2-console` |

---

## Running with Docker

Runs with PostgreSQL. Flyway manages schema migrations.

```bash
cd java
docker compose up --build
```

| Service | Port | URL |
|---|---|---|
| API | 8080 | `http://localhost:8080` |
| PostgreSQL | 5432 | `localhost:5432` (user: `admin`, password: `123`, db: `craftalism`) |

---

## API Reference

Base path: `/api`. Full interactive documentation is available at `http://localhost:8080/api-docs` when running locally.

### Players

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/players` | public | List all players. |
| `GET` | `/players/{uuid}` | public | Get player by UUID. |
| `GET` | `/players/name/{name}` | public | Get player by display name. |
| `POST` | `/players` | `api:write` | Register a new player. |

### Balances

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/balances` | public | List all balances. |
| `GET` | `/balances/{uuid}` | public | Get a player's balance. |
| `GET` | `/balances/top` | public | Top balances. `?limit=` clamped to 1–20, default 10. |
| `POST` | `/balances` | `api:write` | Create a balance record for a player. |
| `PUT` | `/balances/{uuid}/set` | `api:write` | Overwrite a player's balance. |
| `POST` | `/balances/{uuid}/deposit` | `api:write` | Add funds to a player's balance. |
| `POST` | `/balances/{uuid}/withdraw` | `api:write` | Deduct funds from a player's balance. |
| `POST` | `/balances/transfer` | `api:write` | Atomically transfer funds from one player to another. |

### Transactions

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/transactions` | public | List all transactions. |
| `GET` | `/transactions/{id}` | public | Get transaction by ID. Legacy alias `/transactions/id/{id}` is also accepted. |
| `GET` | `/transactions/from/{uuid}` | public | List outgoing transactions for a player. |
| `GET` | `/transactions/to/{uuid}` | public | List incoming transactions for a player. |
| `POST` | `/transactions` | `api:write` | Store a transaction record. Does not update balances. |

---

## Testing

```bash
cd java
./gradlew test
```

The test suite includes unit tests and Spring MVC integration tests. Tests run against H2 with mock security tokens where needed.

---

## Project Structure

```text
java/
├── build.gradle
├── docker-compose.yml
└── src/
    ├── main/java/io/github/HenriqueMichelini/craftalism/api/
    │   ├── Application.java
    │   ├── config/
    │   ├── controller/
    │   ├── dto/
    │   ├── exceptions/
    │   ├── mapper/
    │   ├── model/
    │   ├── repository/
    │   └── service/
    └── main/resources/
        ├── application.properties
        ├── application-local.properties
        ├── application-docker.properties
        └── db/migration/
```

---

## Known Limitations

- Transaction recording and balance transfer are still separate calls; there is no endpoint that atomically updates balances and persists a transaction record together.
- List endpoints have no pagination or filtering support; all records are returned in a single response.
- Integration tests do not run against a real PostgreSQL instance.
- No CI pipeline is configured.

---

## Roadmap

- Integrate transaction creation with `BalanceService.transfer()` so the ledger and balances are updated atomically.
- Add pagination and filtering to all list endpoints.
- Add integration tests against real PostgreSQL with actual security tokens.
- Add CI pipeline with lint, typecheck, build, and test stages.

---

## License

MIT. See [`LICENSE`](./LICENSE) for details.
