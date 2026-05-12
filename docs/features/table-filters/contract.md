# Table Filters API Contract Evidence

## Purpose

Define the implemented API-side filtering contract required to unblock
`craftalism-dashboard` table filter `CARD-001`.

The dashboard needs stable backend-owned semantics before it can build API-backed filter controls for Transactions and Market Trades. This repository owns the API behavior, validation, pagination, sorting, response shapes, and backend tests for the affected endpoints.

## Scope

Affected API resources:

- `GET /api/transactions`
- `GET /api/market/trades`

This document records repo-local API behavior implemented by
`craftalism-api`. Shared-root contract publication remains owned outside this
repository.

## Existing API Evidence

`GET /api/market/trades` supports pageable, filterable reads with:

- `playerUuid`
- `playerUuidMatch`
- `itemId`
- `itemIdMatch`
- `side`
- `minTotalPrice`
- `maxTotalPrice`
- `executedFrom`
- `executedTo`
- Spring Data `page`, `size`, and repeated `sort=property,direction`

`GET /api/transactions` returns a paged, filterable
`Page<TransactionResponseDTO>` for dashboard table reads. Separate transaction
detail and sender/receiver lookup routes remain available.

## Query Parameter Format

All filters are optional and composable. Filters apply before pagination and sorting.

Text matching uses per-field match mode parameters:

- `fromPlayerUuidMatch`
- `toPlayerUuidMatch`
- `playerUuidMatch`
- `itemIdMatch`

Accepted match modes:

- `contains`
- `exact`

Default match mode is `contains` when a text filter value is present and the matching mode parameter is omitted.

Text `contains` comparisons are case-insensitive for string fields. UUID fields support `contains` against the canonical lowercase UUID string representation. `exact` UUID comparisons require a valid UUID value.

Date/time bounds use ISO-8601 instants with offsets or `Z`. Bounds are inclusive and compared in UTC.

Numeric bounds are inclusive.

## Transactions Filters

Endpoint:

```text
GET /api/transactions
```

Filterable fields:

- `fromPlayerUuid`
- `fromPlayerUuidMatch`
- `toPlayerUuid`
- `toPlayerUuidMatch`
- `minAmount`
- `maxAmount`
- `createdFrom`
- `createdTo`

Pagination:

- `page`: zero-based page index
- `size`: page size

Sorting:

- `sort`: repeated `property,direction` values
- allowed properties: `id`, `fromPlayerUuid`, `toPlayerUuid`, `amount`, `createdAt`
- default sort: `createdAt,DESC`, then `id,DESC`

Response shape:

- `Page<TransactionResponseDTO>` JSON shape for `GET /api/transactions`
- Empty filtered and unfiltered results return HTTP `200` with an empty `content` array and page metadata.

Valid examples:

```text
GET /api/transactions?fromPlayerUuid=550e8400&fromPlayerUuidMatch=contains&page=0&size=20
GET /api/transactions?toPlayerUuid=550e8400-e29b-41d4-a716-446655440001&toPlayerUuidMatch=exact
GET /api/transactions?minAmount=100&maxAmount=5000&createdFrom=2026-05-01T00:00:00Z&createdTo=2026-05-12T23:59:59Z
GET /api/transactions?sort=createdAt,desc&sort=id,desc
```

## Market Trade Filters

Endpoint:

```text
GET /api/market/trades
```

Filterable fields:

- `side`
- `playerUuid`
- `playerUuidMatch`
- `itemId`
- `itemIdMatch`
- `minTotalPrice`
- `maxTotalPrice`
- `executedFrom`
- `executedTo`

Dashboard terminology:

- The API accepts canonical `side` only.
- Dashboard-owned `type` terminology maps to `side` outside this repository.

Accepted `side` values:

- `BUY`
- `SELL`

Pagination:

- `page`: zero-based page index
- `size`: page size

Sorting:

- `sort`: repeated `property,direction` values
- allowed properties: `id`, `playerUuid`, `itemId`, `side`, `quantity`, `unitPrice`, `totalPrice`, `executedAt`
- default sort: `executedAt,DESC`, then `id,DESC`

Response shape:

- `Page<MarketTradeHistoryDTO>` JSON shape for `GET /api/market/trades`
- Empty filtered and unfiltered results return HTTP `200` with an empty `content` array and page metadata.

Valid examples:

```text
GET /api/market/trades?side=BUY&page=0&size=20
GET /api/market/trades?side=BUY&playerUuid=550e8400&playerUuidMatch=contains
GET /api/market/trades?itemId=wheat&itemIdMatch=exact&minTotalPrice=1000&maxTotalPrice=50000
GET /api/market/trades?executedFrom=2026-05-01T00:00:00Z&executedTo=2026-05-12T23:59:59Z
```

## Invalid Or Unsupported Filter Behavior

Invalid filter values return the repository-standard validation error response as `ProblemDetail` with HTTP `400`.

Invalid values include:

- unknown match mode
- malformed UUID for exact UUID filters
- malformed ISO-8601 date/time values
- negative numeric bounds
- `minAmount > maxAmount`
- `minTotalPrice > maxTotalPrice`
- `createdFrom > createdTo`
- `executedFrom > executedTo`
- unsupported enum value for `side`
- unsupported sort property
- unsupported sort direction

Unsupported query parameters should be ignored by default unless the implementation card explicitly chooses strict rejection and updates this contract.

## Error Handling

Validation errors use the repository-standard validation `ProblemDetail` shape:

- status `400`
- validation type
- request path
- timestamp
- field-level details when Spring binding or bean validation can identify fields

Unexpected errors retain the repository-standard internal error response and must not leak implementation details.

## Security And Authorization

Both affected endpoints are public reads under the current MVP `GET /api/**` policy:

- `GET /api/transactions`
- `GET /api/market/trades`

Adding filters must not weaken write-scope requirements for protected write routes. Read endpoints must not mutate transaction, market, quote, balance, or transfer state.

## Backward Compatibility

- Existing transaction detail and sender/receiver lookup routes remain available.
- Existing unfiltered list requests remain supported.
- `GET /api/market/trades` keeps its current canonical filter names and `Page<MarketTradeHistoryDTO>` response shape.
- `GET /api/transactions` now returns the repository-standard `Page<TransactionResponseDTO>` JSON shape for list reads.

## Testing Strategy

Controller tests should cover request binding, default values, invalid values, and service invocation.

Service/repository tests should cover:

- optional composable filters
- exact and contains text modes
- inclusive numeric ranges
- inclusive instant ranges
- filters before pagination
- default sorting
- explicit sorting
- empty result shape

Contract/integration tests should cover representative HTTP requests for both endpoints.

Security tests should confirm public read access remains available and write authorization remains unchanged.

## API Implementation Cards

- `CARD-001`: Add transaction table filter contract and request model.
- `CARD-002`: Implement transaction filtered pageable reads.
- `CARD-003`: Extend market trade history filters for dashboard table controls.
- `CARD-004`: Prepare shared table filter contract evidence for dashboard consumption.

## Shared Contract Follow-Up

Shared-root contract changes are outside this repository. The owning
shared-contract repository should promote this API evidence for
`craftalism-dashboard` consumption, including:

- `GET /api/transactions` filter query parameters, validation behavior,
  pagination, sorting, and `Page<TransactionResponseDTO>` response shape.
- `GET /api/market/trades` filter query parameters, validation behavior,
  pagination, sorting, and `Page<MarketTradeHistoryDTO>` response shape.
- The rule that API filters apply before pagination and sorting.
- Canonical API `side` values `BUY` and `SELL`; dashboard-owned `type`
  terminology remains a dashboard mapping, not an API alias.

## Assumptions

- Spring Data `Page<T>` is the repository-standard response shape for pageable table endpoints.
- `contains` on UUID fields is acceptable for dashboard operator search, implemented against the canonical UUID string form.
- API canonical naming remains backend-oriented while dashboard maps display terminology locally.
- Monetary values used in market trade history remain string-encoded integer values in responses.

## Open Questions

- Should unsupported query parameters remain ignored by Spring MVC defaults or be rejected explicitly for stricter dashboard feedback?
