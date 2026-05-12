# Market Trade History Contract

## Purpose

Define repo-local backend ownership, stable rules, and validation boundaries for market-specific trade history.

## Source of Truth

- `../../market-contract-mvp.md` defines the public market API contract, including trade-history endpoint shapes and access rules.
- `../market-pressure-ladder/contract.md` defines authoritative quote and execute semantics that trade history must preserve.

## Repository Ownership

`craftalism-api` owns market trade-history persistence, read API behavior, access policy, response DTOs, filtering, pagination, and backend tests.

`craftalism-dashboard` consumes the trade-history API and must be planned separately after the API response shape and endpoint semantics are stable.

## Goals

- Persist completed market executions as immutable market trade history records.
- Expose read-only market trade history endpoints for dashboard and ops use.
- Allow paged and filtered trade-history reads by player, item, side, and execution time range.
- Expose detail lookup for one trade history record by id.
- Keep trade-history records aligned with committed successful `/api/market/execute` outcomes.

## Non-Goals

- Do not implement dashboard or client behavior in this repository.
- Do not expose quotes, rejected attempts, pending quotes, expired quotes, or quote lifecycle records as trade history.
- Do not redefine market quote, execute, pressure-ladder, balance, or transfer semantics.

## Domain Rules

- A trade history record represents one committed successful market execution.
- Records include `id`, `playerUuid`, `itemId`, `side`, `quantity`, `unitPrice`, `totalPrice`, `currency`, `snapshotVersion`, and `executedAt`.
- `side` uses the existing market side values.
- Monetary values use the same string-encoded integer representation as market quote and execute DTOs.
- `snapshotVersion` remains opaque and must not be parsed by clients.
- `executedAt` is the backend execution timestamp for the committed trade record.

## Persistence Rules

- Persist the trade history record in the same transaction as successful `/api/market/execute`.
- Persist the record only after the trade has been applied successfully.
- If execute returns a rejection or settlement fails, no trade history record is written.
- Read endpoints expose only committed successful executions.
- Trade history records are append-only for normal API behavior.

## External Interfaces

- `GET /api/market/trades`
- `GET /api/market/trades/{id}`
- Market trade history list item DTOs
- Market trade history detail DTOs
- Flyway migrations for durable state changes

## Filtering and Paging Rules

`GET /api/market/trades` supports:

- `playerUuid`
- `playerUuidMatch`
- `itemId`
- `itemIdMatch`
- `side`
- `minTotalPrice`
- `maxTotalPrice`
- `executedFrom`
- `executedTo`
- pageable request parameters

Filters are optional and composable. Text match modes accept `contains` and
`exact`; default mode is `contains` when a text filter is present and its match
mode is omitted. UUID `exact` matching requires a valid UUID value. `side` uses
canonical `BUY` or `SELL`; dashboard-owned `type` terminology is not an API
alias. `minTotalPrice` and `maxTotalPrice` are inclusive non-negative integer
bounds. `executedFrom` and `executedTo` are inclusive instant bounds.

Pageable requests use Spring Data pageable query parameters:

- `page`: zero-based page index
- `size`: page size
- `sort`: repeated `property,direction` values

List responses use the repository-standard Spring `Page<MarketTradeHistoryDTO>` JSON shape. The default ordering is newest first by `executedAt,DESC`, then `id,DESC` for deterministic ordering when multiple trades share the same execution timestamp.

## Security and Permission Rules

- Trade-history endpoints are public read endpoints under the current MVP `GET /api/**` policy.
- `GET /api/market/trades` and `GET /api/market/trades/{id}` do not require a bearer token.
- Do not weaken existing write-scope requirements for quote or execute endpoints.

## Error and Failure Rules

- Unknown trade history ids return the repository-standard not-found response for read lookups.
- Invalid filter values return the repository-standard validation error response.
- Trade-history reads must not mutate market state, quote state, balance state, or transfer state.

## Cross-Feature Dependencies

- Market execute success semantics come from `../market-pressure-ladder/contract.md`.
- Balance settlement and transfer behavior remain authoritative in this repository.
- Dashboard consumption is out of scope until API behavior is implemented and stable.

## Public Contract Change Rules

Changes to trade-history endpoints, DTOs, filters, persistence, permissions, or external behavior require explicit card scope.

## Validation Rules

Use the validation command listed by the selected card. Prefer focused tests for narrow cards and `rtk ./gradlew test` from `java/` when the card changes cross-service market behavior.

## Source Areas

- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/SecurityConfig.java`
- `java/src/main/resources/db/migration/`

## Test Areas

- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/security/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/`
