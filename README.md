# Craftalism API

> Core backend service for players, balances, transfers, transaction history, and the dynamic Craftalism market.

---

## Overview

The Craftalism API is the central data service for the economy platform. It exposes REST endpoints consumed by both the Craftalism Dashboard and the Minecraft plugin. All write operations require a valid JWT issued by the Craftalism Authorization Server.

**Key capabilities:**

- Player registration and lookup by UUID or display name.
- Balance lifecycle management: create, deposit, withdraw, set, delete, rank, and transfer.
- Transaction record storage between players.
- Dynamic market snapshots, quote-backed execution, trade history, event scheduling, and dashboard administration.
- JWT scope-based authorization for protected writes and market-administration routes.
- Standardized error responses, including RFC 9457 `ProblemDetail` for general API errors and a stable rejection payload for market business rejections.
- OpenAPI documentation and Swagger UI.

> **Important:** `POST /api/transactions` stores a transaction record only. It does **not** transfer balances. Use `POST /api/balances/transfer` for atomic balance movement **with transaction persistence and idempotency key support**.

---

## Architecture

The API combines shared HTTP and persistence layers with feature-oriented application and domain packages:

- `controller/` exposes REST endpoints under `/api/**`, validates request DTOs with Bean Validation, and returns typed DTO responses.
- `player/application/`, `wallet/application/`, `transaction/application/`, and `transfer/application/` contain feature-specific use cases and transactional behavior.
- `market/` is split into application commands/queries, domain pricing and trade rules, and infrastructure for catalog bootstrap, quote storage, configuration, and event scheduling.
- `repository/` contains Spring Data JPA repositories, including locking queries used by balance transfers and market execution.
- `model/` contains the JPA entities for players, balances, transactions, transfers, and the market.
- `config/`, `exceptions/`, `mapper/`, and `shared/` provide security, error handling, DTO mapping, and reusable table filtering.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.7 |
| Web | Spring Web |
| Persistence | Spring Data JPA |
| Validation | Spring Validation |
| Security | Spring Security + OAuth2 Resource Server (JWT) |
| Database (Docker profile) | PostgreSQL |
| Database (local profile) | H2 in-memory |
| Migrations | Flyway (Docker profile only) |
| API Docs | springdoc-openapi (Swagger UI) |
| Build Tool | Gradle 8.14.3 wrapper |
| Testing | JUnit 5, Mockito, Spring Test |

---

## Prerequisites

- Java 17+
- A reachable OAuth2/OIDC authorization server whose issuer matches `AUTH_ISSUER_URI`.
- Docker Engine 20.10+ and Docker Compose v2+ *(for containerized deployment only)*

---

## Configuration

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — | Select `local` for H2 or `docker` for PostgreSQL. The commands below set the appropriate profile. |
| `AUTH_ISSUER_URI` | `http://localhost:9000` (local) / `http://craftalism-auth-server:9000` (docker) | JWT issuer URI. Must match the Authorization Server's configured issuer. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/craftalism` | JDBC connection string *(Docker profile)*. |
| `SPRING_DATASOURCE_USERNAME` | `admin` | Database username *(Docker profile)*. |
| `SPRING_DATASOURCE_PASSWORD` | `123` | Database password *(Docker profile)*. |
| `MARKET_QUOTE_RATE_LIMIT_MAX_REQUESTS` | `0` | Maximum quotes per player and rate-limit window; `0` disables this limit. |
| `MARKET_EXECUTE_RATE_LIMIT_MAX_REQUESTS` | `0` | Maximum executions per player and rate-limit window; `0` disables this limit. |
| `MARKET_RATE_LIMIT_WINDOW_SECONDS` | `60` | Rate-limit window length. |

Additional settings are managed per profile:

- `src/main/resources/application.properties` — shared defaults.
- `src/main/resources/application-local.properties` — H2, create-drop schema, Flyway disabled.
- `src/main/resources/application-docker.properties` — PostgreSQL, Flyway enabled.

The market also supports these Spring properties:

| Property | Default | Description |
|---|---:|---|
| `craftalism.market.enabled` | `true` | Enables quote and execute operations. |
| `craftalism.market.quote-ttl-seconds` | `60` | Quote validity period. |
| `craftalism.market.trusted-minecraft-server-client-id` | `minecraft-server` | Client allowed to provide another player's UUID for market operations. |
| `craftalism.market-events.scheduler.enabled` | `true` | Enables automatic market-event selection. |
| `craftalism.market-events.scheduler.start-chance-basis-points` | `2500` | Chance of starting an event when a scheduler window is due. |
| `craftalism.market-events.scheduler.check-delay-ms` | `300000` | Delay between scheduler checks. |
| `craftalism.market-events.scheduler.initial-delay-ms` | `300000` | Delay before the first scheduler check. |
| `craftalism.market-events.scheduler.lease-seconds` | `60` | Distributed scheduler lease duration. |
| `craftalism.market-events.scheduler.window-interval-seconds` | `7200` | Base interval between event windows. |
| `craftalism.market-events.scheduler.window-jitter-seconds` | `1800` | Maximum random jitter added to an event window. |

### Security model

The API is a stateless OAuth2 resource server. JWTs are validated against the issuer URI.

| Scope | Permitted methods |
|---|---|
| *(no scope required)* | `GET /api/**` (public read policy for current MVP) |
| `SCOPE_api:write` | `POST`, `PUT`, `PATCH`, `DELETE` on `/api/**` |
| `SCOPE_market:admin` | All `/api/dashboard/market/events/**`, `/api/dashboard/market/event-templates/**`, and `/api/dashboard/market/drift/**` requests, including reads. |

The admin route rules take precedence over the general public-read and `api:write` rules. Dashboard category and item reads remain public; their writes require `api:write`.

Public paths (no token required): `/actuator/health`, `/swagger-ui/**`, and `/v3/api-docs/**`. CORS allows `http://localhost:5173`, `http://localhost:5174`, and `http://localhost:25565`.

### Error contract

General API errors are returned as RFC 9457 `ProblemDetail` with these additional fields:

| Field | Description |
|---|---|
| `type` | One of: `.../validation`, `.../business-rule`, `.../internal` |
| `detail` | Human-readable error description. |
| `status` | HTTP status code. |
| `timestamp` | ISO 8601 timestamp. |
| `path` | Request path. |
| `errors` | Field-level validation map (validation errors only). |

Market quote and execute business rejections use the owned market rejection JSON contract instead of `ProblemDetail`:

| Field | Description |
|---|---|
| `status` | Rejection status, always `REJECTED`. |
| `code` | Stable machine-readable rejection code. |
| `message` | Human-readable rejection message. |
| `snapshotVersion` | Latest authoritative market snapshot token. |

### Troubleshooting quick checks

- **Issuer mismatch at startup**  
  If the API fails fast with an issuer mismatch error, verify `AUTH_ISSUER_URI` is aligned between API, auth server, and deployment environment.
- **Transfer failures with incident warnings**  
  Transfer incident recording is diagnostic. If incident persistence fails, the API logs a critical error and preserves the original transfer response semantics.

---

## Running Locally

Runs with H2 in-memory database. Schema is recreated on each startup. Flyway is disabled.

```bash
cd java
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

The authorization server must be available at `http://localhost:9000`, or set `AUTH_ISSUER_URI` to its issuer before starting the API.

| Endpoint | URL |
|---|---|
| API | `http://localhost:8080` |
| Health | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

The local profile configures the aliases `/api-docs` and `/h2-console`, but the current security rules do not make those paths public. Use the public Swagger URL above.

---

## Running with Docker

Runs with PostgreSQL. Flyway manages schema migrations.

```bash
cd java
docker compose up --build
```

Set `AUTH_ISSUER_URI` to an issuer reachable from the API container unless a `craftalism-auth-server` host is already available on the Compose network:

```bash
AUTH_ISSUER_URI=http://auth-server:9000 docker compose up --build
```

Replace `auth-server` with a hostname or address resolvable from the Compose network, or attach the authorization-server container to that network.

| Service | Port | URL |
|---|---|---|
| API | 8080 | `http://localhost:8080` |
| PostgreSQL | 5432 | `localhost:5432` (user: `admin`, password: `123`, db: `craftalism`) |

The checked-in Compose file sets `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, overriding the Docker profile's `validate` setting while Flyway remains enabled. The Docker image health check currently probes `/health`, although the application exposes `/actuator/health`; the container can therefore be reported as unhealthy even when the API is serving requests.

---

## API Reference

Base path: `/api`. Interactive documentation is available at `http://localhost:8080/swagger-ui/index.html` when running locally.

### Players

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/players` | public | List all players. |
| `GET` | `/players/{uuid}` | public | Get player by UUID. |
| `GET` | `/players/name/{name}` | public | Get player by display name. |
| `POST` | `/players` | `api:write` | Register a new player. |
| `PATCH` | `/players/{uuid}` | `api:write` | Update a player's display name. |
| `DELETE` | `/players/{uuid}` | `api:write` | Delete an unreferenced player. |

### Balances

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/balances` | public | List all balances. |
| `GET` | `/balances/{uuid}` | public | Get a player's balance. |
| `GET` | `/balances/top` | public | Top balances. `limit` defaults to 10, values above 20 are capped at 20, and non-positive values use the default. |
| `POST` | `/balances` | `api:write` | Create a balance record for a player. |
| `PUT` | `/balances/{uuid}/set` | `api:write` | Overwrite a player's balance. |
| `PATCH` | `/balances/{uuid}` | `api:write` | Overwrite a player's balance. |
| `DELETE` | `/balances/{uuid}` | `api:write` | Delete a balance record. |
| `POST` | `/balances/{uuid}/deposit` | `api:write` | Add funds to a player's balance. |
| `POST` | `/balances/{uuid}/withdraw` | `api:write` | Deduct funds from a player's balance. |
| `POST` | `/balances/transfer` | `api:write` | Atomically transfer funds and store the transaction. Requires `Idempotency-Key`. |

### Transactions

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/transactions` | public | Return paginated transactions with optional filters. |
| `GET` | `/transactions/{id}` | public | Get transaction by ID. Legacy alias `/transactions/id/{id}` is also accepted. |
| `GET` | `/transactions/from/{uuid}` | public | List outgoing transactions for a player. |
| `GET` | `/transactions/to/{uuid}` | public | List incoming transactions for a player. |
| `POST` | `/transactions` | `api:write` | Store a transaction record. Does not update balances. |

### Market

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/market/snapshot` | public | Return the authoritative market snapshot with an opaque `snapshotVersion`. |
| `GET` | `/market/trades` | public | Return paginated committed market executions with optional filters. |
| `GET` | `/market/trades/{id}` | public | Get one committed successful market execution. |
| `POST` | `/market/quotes` | `api:write` | Create a quote-backed market trade for the authenticated player context. |
| `POST` | `/market/execute` | `api:write` | Execute a trade using a required `quoteToken` and `snapshotVersion`. |

Market prices are exposed as string-encoded whole-coin amounts. Business rejections use a stable JSON contract with machine-readable codes instead of free-form text.

`GET /transactions` and `GET /market/trades` accept Spring pagination parameters (`page`, `size`, and `sort`). Transaction filters include sender/receiver UUID, amount range, and creation-time range. Market-trade filters include player UUID, item ID, side, total-price range, and execution-time range. String filters support a companion `*Match` parameter with `contains` (default) or `exact`.

Market snapshot and execute `updatedItem` payloads expose pressure-ladder state with `marketPressure`, `marketSegment`, and `pressureMagnitude`. The superseded `currentStock` field is not part of the public pressure-ladder contract. `INSUFFICIENT_STOCK` is reserved for trades that would exceed configured hard pressure bounds (`maxNetPosition` for buys or `minNetPosition` for sells).

Market `snapshotVersion` values are opaque `market:<hash>` tokens derived from authoritative pressure state and trade-affecting configuration. Clients must compare or pass them through only; they must not parse token structure or infer inventory semantics from it.

Market player context is resolved from a valid JWT `player_uuid` claim first, then a UUID-valued subject. When the authenticated client is the configured trusted Minecraft server client (`minecraft-server`, recognized from JWT `sub`, `client_id`, or `azp`) with `api:write`, quote and execute may instead supply the Bukkit player UUID as request field `playerUuid` or header `X-Craftalism-Player-Uuid`. Supplied player UUIDs are ignored for non-trusted clients and must be valid UUIDs.

On startup, the service seeds the built-in market catalog and initial event templates only when the corresponding tables are empty.

### Dashboard market administration

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/dashboard/market/categories` | public | List market categories. |
| `POST` | `/dashboard/market/categories` | `api:write` | Create a category. |
| `PATCH` | `/dashboard/market/categories/{categoryId}` | `api:write` | Update a category. |
| `DELETE` | `/dashboard/market/categories/{categoryId}` | `api:write` | Delete an unused category. |
| `GET` | `/dashboard/market/items` | public | List market items. |
| `POST` | `/dashboard/market/items` | `api:write` | Create an item. |
| `PATCH` | `/dashboard/market/items/{itemId}` | `api:write` | Update an item. |
| `DELETE` | `/dashboard/market/items/{itemId}` | `api:write` | Delete an unused, dashboard-managed item. |
| `GET` | `/dashboard/market/event-templates` | `market:admin` | List event templates. |
| `POST` | `/dashboard/market/event-templates` | `market:admin` | Create an event template. |
| `PUT` | `/dashboard/market/event-templates/{templateId}` | `market:admin` | Replace an event template. |
| `DELETE` | `/dashboard/market/event-templates/{templateId}` | `market:admin` | Delete an event template. |
| `GET` | `/dashboard/market/events` | `market:admin` | List market events. |
| `POST` | `/dashboard/market/events` | `market:admin` | Start a manual event. |
| `PATCH` | `/dashboard/market/events/{id}` | `market:admin` | Update an event. |
| `POST` | `/dashboard/market/events/{id}/cancel` | `market:admin` | Cancel an event. |
| `POST` | `/dashboard/market/events/supersede` | `market:admin` | Replace the active event. |
| `POST` | `/dashboard/market/drift/reset` | `market:admin` | Reset drift for all market items. |

### Transfer incidents (diagnostic)

Incident records are persisted by the transfer workflow for failure/conflict diagnostics (for example idempotency conflicts and unexpected transfer failures). Records can be queried through:

| Method | Path | Scope | Description |
|---|---|---|---|
| `GET` | `/transfer-incidents` | public | List recorded transfer incidents with incident type, reason, metadata, and idempotency correlation context. |

---

## Testing

```bash
cd java
./gradlew test
```

The test suite includes unit tests and Spring MVC integration tests. Tests run against H2 with mock security tokens where needed.

To compile, test, and build the executable JAR:

```bash
cd java
./gradlew build
```

## SonarQube Analysis

The Gradle build applies the SonarQube scanner and JaCoCo. Java analysis is performed by the SonarJava analyzer installed in the target SonarQube/SonarCloud server; the Gradle scanner sends compiled bytecode, JUnit results, and JaCoCo XML coverage.

Project key: `craftalism-api-key`.

```bash
cd java
SONAR_TOKEN=<token> SONAR_HOST_URL=<sonarqube-url> SONAR_ORGANIZATION=<organization> ./gradlew sonar
```

`SONAR_HOST_URL` is optional when using the default SonarCloud endpoint configured for this project. `SONAR_ORGANIZATION` is only needed for SonarCloud or SonarQube setups that require it; when no host URL is provided, the project defaults to the `henriquemichelini` organization. In GitHub Actions, configure `SONAR_TOKEN` and optionally `SONAR_HOST_URL`/`SONAR_ORGANIZATION` as repository secrets to enable the SonarQube analysis step in the quality-gates workflow.

---

## Project Structure

```text
java/
├── build.gradle
├── docker-compose.yml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/io/github/HenriqueMichelini/craftalism/api/
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── exceptions/
    │   │   ├── market/
    │   │   ├── model/
    │   │   ├── player/
    │   │   ├── repository/
    │   │   ├── shared/
    │   │   ├── transaction/
    │   │   ├── transfer/
    │   │   └── wallet/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-local.properties
    │       ├── application-docker.properties
    │       └── db/migration/
    └── test/
```

---

## Known Limitations

- Only the transaction and market-trade list endpoints support pagination and filtering; other list endpoints return all matching records.
- Integration tests do not run against a real PostgreSQL instance.
- Repository workflows currently enforce checks, but branch protection/required status enforcement is configured outside this repository.

---

## License

MIT. See [`LICENSE`](./LICENSE) for details.
