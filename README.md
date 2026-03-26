# Craftalism API

Core backend API for the Craftalism ecosystem. This service manages **players**, **balances**, and **transactions** through a secured REST API built with Spring Boot.

---

## What this project does

Craftalism API provides:
- Player registration and lookup by UUID or name
- Balance lifecycle and wallet-like operations (create, set, deposit, withdraw)
- Transaction records between players
- OpenAPI/Swagger documentation
- JWT scope-based authorization for read/write access
- Standardized error responses using `ProblemDetail`

> Important implementation note: the current `POST /api/transactions` endpoint **stores a transaction record only**. It does **not** call the balance transfer logic in `BalanceService.transfer(...)`. Balances must currently be changed via the balance endpoints.

---

## Tech stack

- **Java 17**
- **Spring Boot 3.5.x**
  - Spring Web
  - Spring Data JPA
  - Spring Validation
  - Spring Security + OAuth2 Resource Server (JWT)
  - Actuator
- **PostgreSQL** (Docker profile)
- **H2 (in-memory)** (local profile)
- **Flyway** (enabled in Docker/Postgres profile)
- **springdoc-openapi** (Swagger UI)
- **JUnit 5 + Mockito + Spring Test**

Build tool: **Gradle**.

---

## Architecture overview

The codebase follows a classic layered architecture:

1. **Controller layer** (`controller/`)
   - Exposes REST endpoints under `/api/**`
   - Validates request DTOs
   - Returns DTO responses and HTTP status codes

2. **Service layer** (`service/`)
   - Encapsulates business rules and transactional behavior
   - Throws typed business exceptions for domain errors

3. **Repository layer** (`repository/`)
   - Spring Data JPA repositories
   - Includes custom queries for top balances and pessimistic locking

4. **Domain/model layer** (`model/`)
   - JPA entities: `Player`, `Balance`, `Transaction`

5. **Cross-cutting concerns**
   - `config/`: Security and OpenAPI configuration
   - `exceptions/`: centralized exception mapping and problem details
   - `mapper/`: entity-to-DTO mapping components

---

## Security model

- API is configured as **stateless resource server**.
- JWTs are validated using issuer URI from `AUTH_ISSUER_URI` (default: `http://localhost:9000`).
- Authorization is scope-based:
  - `GET /api/**` requires `SCOPE_api:read`
  - `POST|PUT|PATCH|DELETE /api/**` requires `SCOPE_api:write`
- Public endpoints:
  - `GET /actuator/health`
  - Swagger endpoints (`/swagger-ui/**`, `/v3/api-docs/**`)

---

## Error contract

Errors are returned as RFC-9457-style `ProblemDetail` payloads with additional properties:

- `type`:
  - `https://api.craftalism.com/errors/validation`
  - `https://api.craftalism.com/errors/business-rule`
  - `https://api.craftalism.com/errors/internal`
- `detail`
- `status`
- `timestamp`
- `path`
- `errors` (field-level validation map, when applicable)

---

## API overview

Base path: `/api`

### Players
- `GET /players` — list all players
- `GET /players/{uuid}` — get player by UUID
- `GET /players/name/{name}` — get player by name
- `POST /players` — create player

### Balances
- `GET /balances` — list all balances
- `GET /balances/{uuid}` — get balance by player UUID
- `POST /balances` — create balance
- `PUT /balances/{uuid}/set` — overwrite balance amount
- `POST /balances/{uuid}/deposit` — deposit funds
- `POST /balances/{uuid}/withdraw` — withdraw funds
- `GET /balances/top?limit=10` — top balances (limit clamped to 1..20, defaults to 10)

### Transactions
- `GET /transactions` — list all transactions
- `GET /transactions/id/{id}` — get transaction by ID
- `GET /transactions/from/{uuid}` — list outgoing transactions for player
- `GET /transactions/to/{uuid}` — list incoming transactions for player
- `POST /transactions` — create a transaction record

Swagger UI path (local profile): `http://localhost:8080/api-docs`

---

## Running the project

## Prerequisites

- Java 17
- Docker + Docker Compose (optional, for Postgres run mode)

### Option A: Run locally with H2 (fast dev mode)

From repository root:

```bash
cd java
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Then access:
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/api-docs`
- H2 console: `http://localhost:8080/h2-console`

Notes:
- In this profile Flyway is disabled.
- Schema is recreated on startup/shutdown (`create-drop`).

### Option B: Run with Docker Compose + PostgreSQL

From repository root:

```bash
cd java
docker compose up --build
```

Then access:
- API: `http://localhost:8080`
- Postgres: `localhost:5432` (`admin` / `123`, DB `craftalism`)

Notes:
- Flyway migrations are enabled in Docker profile.
- `AUTH_ISSUER_URI` defaults to `http://craftalism-auth-server:9000` in Compose and can be overridden.

---

## Configuration

Main environment/property knobs:

- `SPRING_PROFILES_ACTIVE` (`local` or `docker`)
- `AUTH_ISSUER_URI` (JWT issuer)
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

See:
- `java/src/main/resources/application.properties`
- `java/src/main/resources/application-local.properties`
- `java/src/main/resources/application-docker.properties`

---

## Project structure

```text
.
├── README.md
├── java/
│   ├── build.gradle
│   ├── docker-compose.yml
│   └── src/
│       ├── main/java/io/github/HenriqueMichelini/craftalism/api/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── exceptions/
│       │   ├── mapper/
│       │   ├── model/
│       │   ├── repository/
│       │   ├── service/
│       │   └── Application.java
│       ├── main/resources/
│       │   ├── application*.properties
│       │   └── db/migration/
│       └── test/java/... (unit and MVC tests)
```

---

## Current limitations / next improvements

- Integrate transaction creation with atomic balance transfer (`BalanceService.transfer`) so ledger and balances are updated together.
- Add pagination/filtering for listing endpoints.
- Expand integration tests with real database and security tokens.
- Add CI pipeline and coverage badge in repository root.
