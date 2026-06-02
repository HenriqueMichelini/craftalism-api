# Dashboard CRUD API Contract

## Purpose

Define repo-local backend write contracts used by dashboard resource modals.

## Repository Ownership

`craftalism-api` owns authoritative player, balance, and market item persistence, mutation semantics, and API error behavior.

## Domain Rules

- Player UUIDs are client-supplied on create and are immutable after creation.
- Player UUIDs must be valid UUID values. This API does not enforce a UUID version.
- Player names are required, trimmed before persistence, and unique.
- Balances are keyed by player UUID. `Balance.uuid` is the owning player UUID, not a separate balance identifier.
- There is at most one balance per player.
- Balance amounts are scaled integer `BIGINT` values using the project amount scale convention.
- Balance amounts must be zero or positive for create and set/update operations.
- Market items are keyed by `itemId`.
- Market item `itemId`, `categoryId`, and `displayName` are immutable after creation through dashboard admin routes.
- Market item `lastUpdatedAt` is API-owned and set by the server on create/update.
- Market item `buyUnitEstimate`, `sellUnitEstimate`, `currentStock`, `variationPercent`, and `marketMomentum` are derived pressure-ladder projections. Dashboard admin responses expose them, but create/update requests must not accept them as editable inputs.
- Market item create/update requests edit authoritative pricing, regeneration, pressure, and state controls only.
- Market item deletes are allowed only for non-default items that are not referenced by active quotes. Resolved quotes and trade history remain intact after deletion.

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

### Market Items

- `GET /api/dashboard/market/items`
- `POST /api/dashboard/market/items`
- `PATCH /api/dashboard/market/items/{itemId}`
- `DELETE /api/dashboard/market/items/{itemId}`

## Error Contract

- Business and validation failures use `ProblemDetail`.
- Bean validation failures return `400` with validation type and an `errors` field map.
- Malformed request bodies and invalid UUID values return `400` with validation type.
- Missing resources return `404` with business-rule type.
- Duplicate player UUIDs, duplicate player names, and duplicate balances return `409` with business-rule type.
- Invalid balance amounts rejected by service invariants return `422` with business-rule type.
- Player deletion that would violate existing references returns `409` with business-rule type.
- Duplicate market item IDs return `409` with business-rule type.
- Market item delete attempts for default catalog items or items referenced by active quotes return `409` with business-rule type.
- Market item pressure constraint failures return `400` with validation type.

## Mutation Response Rules

- Create endpoints return `201`, `Location`, and the created resource.
- Update endpoints return `200` and the updated resource.
- Delete endpoints return `204` with no response body.
- Dashboard clients may apply returned create/update resources directly and should remove locally deleted rows after `204`.
- Market item create/update responses include recomputed derived projection fields and the API-owned `lastUpdatedAt` value.

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
