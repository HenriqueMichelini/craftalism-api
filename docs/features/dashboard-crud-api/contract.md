# Dashboard CRUD API Contract

## Purpose

Define repo-local backend write contracts used by dashboard resource modals.

## Repository Ownership

`craftalism-api` owns authoritative player and balance persistence, mutation semantics, and API error behavior.

## Domain Rules

- Player UUIDs are client-supplied on create and are immutable after creation.
- Player UUIDs must be valid UUID values. This API does not enforce a UUID version.
- Player names are required, trimmed before persistence, and unique.
- Balances are keyed by player UUID. `Balance.uuid` is the owning player UUID, not a separate balance identifier.
- There is at most one balance per player.
- Balance amounts are scaled integer `BIGINT` values using the project amount scale convention.
- Balance amounts must be zero or positive for create and set/update operations.

## External Interfaces

### Players

- `GET /api/players`
- `GET /api/players/{uuid}`
- `GET /api/players/name/{name}`
- `POST /api/players`
- `PATCH /api/players/{uuid}`
- `DELETE /api/players/{uuid}`

### Balances

- `GET /api/balances`
- `GET /api/balances/{uuid}`
- `POST /api/balances`
- `PATCH /api/balances/{uuid}`
- `PUT /api/balances/{uuid}/set`
- `POST /api/balances/{uuid}/deposit`
- `POST /api/balances/{uuid}/withdraw`
- `POST /api/balances/transfer`
- `GET /api/balances/top`
- `DELETE /api/balances/{uuid}`

## Error Contract

- Business and validation failures use `ProblemDetail`.
- Bean validation failures return `400` with validation type and an `errors` field map.
- Malformed request bodies and invalid UUID values return `400` with validation type.
- Missing resources return `404` with business-rule type.
- Duplicate player UUIDs, duplicate player names, and duplicate balances return `409` with business-rule type.
- Invalid balance amounts rejected by service invariants return `422` with business-rule type.
- Player deletion that would violate existing references returns `409` with business-rule type.

## Mutation Response Rules

- Create endpoints return `201`, `Location`, and the created resource.
- Update endpoints return `200` and the updated resource.
- Delete endpoints return `204` with no response body.
- Dashboard clients may apply returned create/update resources directly and should remove locally deleted rows after `204`.

## Security Rules

Write endpoints require the existing API write authority. Do not weaken read/write security boundaries.

## Source Areas

- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/`

## Test Areas

- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/`
