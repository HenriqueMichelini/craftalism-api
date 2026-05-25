# CARD-002: Implement Dashboard Market Item CRUD API

## Status

completed

## Objective

Implement dashboard admin CRUD routes for market items so the dashboard can list, create, edit, and remove market item configuration through API-owned contracts.

## Context

The dashboard needs a `Market Items` CRUD table, but market item persistence, pricing, validation, and deletion semantics are authoritative backend behavior owned by this repository.

Market pressure-ladder rules make `buyUnitEstimate`, `sellUnitEstimate`, `currentStock`, `variationPercent`, and `marketMomentum` derived projections. Admin responses may expose them, but create and update requests must edit authoritative inputs and let the API recompute derived projections.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

The API exposes dashboard admin market item CRUD routes under `/api/dashboard/market/items`. Responses use a flat market item admin DTO. Create and update requests accept only authoritative editable fields, set `lastUpdatedAt` server-side, recompute derived projections, and return structured `ProblemDetail` validation or business-rule failures.

## Acceptance Criteria

- [ ] `GET /api/dashboard/market/items` returns a flat `MarketItem[]` admin response ordered by the existing market read order.
- [ ] `POST /api/dashboard/market/items` creates a market item, returns `201`, `Location`, and the created item.
- [ ] `PATCH /api/dashboard/market/items/{itemId}` updates only editable authoritative fields and returns the updated item.
- [ ] `DELETE /api/dashboard/market/items/{itemId}` deletes an unreferenced non-default market item and returns `204`.
- [ ] Delete rejects default catalog items or referenced items with `409 ProblemDetail`.
- [ ] Missing market items return `404 ProblemDetail`.
- [ ] Duplicate `itemId` create requests return `409 ProblemDetail`.
- [ ] Validation failures for required fields and market pressure constraints return `400 ProblemDetail`.
- [ ] `itemId`, `categoryId`, and `displayName` are immutable after creation.
- [ ] `lastUpdatedAt` is set by the API and is never accepted from requests.
- [ ] `buyUnitEstimate`, `sellUnitEstimate`, `currentStock`, `variationPercent`, and `marketMomentum` are response fields recomputed by the API, not editable request fields.
- [ ] Focused integration tests cover list, create, update, delete, validation, duplicate create, missing item, default-item delete rejection, and referenced-item delete rejection.

## Expected Files to Change

```text
docs/features/dashboard-crud-api/contract.md
docs/features/dashboard-crud-api/cards/CARD-002-implement-dashboard-market-item-crud-api.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/mapper/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
```

## Constraints

- Do not change persistence schema.
- Do not change public market snapshot, quote, execute, or trade history route behavior.
- Do not change dashboard frontend code.
- Do not weaken read/write security boundaries.
- Follow `docs/market-pressure-ladder-sigmoid-pricing.md` strictly.

## Validation Commands

```bash
./gradlew test --tests '*DashboardMarketItemCrudApiIntegrationTest' --tests '*DashboardCrudApiIntegrationTest' --tests '*Market*'
```

Fallback if the filtered command is unavailable:

```bash
./gradlew test
```

## Out of Scope

- Dashboard frontend implementation.
- API-backed dashboard filtering.
- Sorting beyond existing market read order.
- Detail pages.
- Auth rollout or security policy changes.
- Schema or migration changes.
- Public market snapshot, quote, execute, or trade history behavior changes.

## Completion Notes

- Implemented dashboard admin market item CRUD routes under `/api/dashboard/market/items`.
- Added admin request/response DTOs, mapper, service, business exceptions, controller, and repository reference checks.
- Market item create/update requests edit authoritative pressure-ladder inputs only; `buyUnitEstimate`, `sellUnitEstimate`, `currentStock`, `variationPercent`, and `marketMomentum` are recomputed response projections.
- Delete rejects default catalog items and items referenced by market quotes or trade history.
- Validated with the filtered card command and full `./gradlew test`.
