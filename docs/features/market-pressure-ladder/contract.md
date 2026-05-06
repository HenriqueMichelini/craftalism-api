# Market Pressure-Ladder Contract

## Purpose

Define repo-local backend ownership, stable rules, and validation boundaries for the market pressure-ladder feature.

## Source of Truth

- `../../market-pressure-ladder-sigmoid-pricing.md` is the authoritative design document.
- `../../aggregate-dynamic-pricing.md` is historical context only.

Follow the source of truth strictly. Do not simplify or reinterpret the pricing model.

## Repository Ownership

`craftalism-api` owns authoritative backend market state, persistence, pricing, quote planning, execution, regeneration, snapshot DTOs, `snapshotVersion`, rejection semantics, catalog defaults, validation, and backend tests.

`craftalism-market` consumes the pressure-ladder contract and must be updated outside this repository after backend contract changes.

## Goals

- Represent market state with pressure-ladder fields on market items.
- Derive deterministic pressure segments and bounded unit prices from `netPosition`.
- Quote and execute trades against virtual pressure positions.
- Regenerate pressure toward equilibrium.
- Expose canonical backend snapshot and execute response semantics.
- Keep stale detection and tests aligned with authoritative pressure state.

## Non-Goals

- Do not implement client behavior in this repository.
- Do not redefine shared cross-repo behavior.
- Do not preserve superseded stock semantics once a selected card explicitly replaces them.
- Do not remove legacy segment storage unless a selected card explicitly scopes it.

## Domain Rules

- Segment derivation uses `Math.floorDiv(netPosition, segmentSize)`.
- Segment `0` prices exactly at `baseUnitPrice`.
- Positive pressure approaches `maxUnitPrice`.
- Negative pressure approaches `minUnitPrice`.
- Unit prices are rounded and clamped within configured bounds.
- BUY quote planning prices positions `netPosition` through `netPosition + quantity - 1`.
- SELL quote planning prices positions `netPosition - 1` through `netPosition - quantity`.
- Ordinary buys and sells are not limited by finite stock.
- `INSUFFICIENT_STOCK` is emitted only for configured hard pressure bounds.
- Regeneration moves positive or negative `netPosition` toward `0`.

## Invariants

- `market_items` is the authoritative pressure-state aggregate.
- `snapshotVersion` is opaque to clients.
- Failed settlement must not mutate pressure state.
- Quote single-use and stale quote semantics must be preserved.
- `craftalism-api` remains authoritative for snapshot, quote, execute, blocked state, operating state, and version semantics.

## External Interfaces

- `GET /api/market/snapshot`
- `POST /api/market/quotes`
- `POST /api/market/execute`
- Market snapshot item DTOs
- Market execute success DTOs
- Market rejection codes and RFC 9457-style error semantics
- Flyway migrations for durable state changes

## Cross-Feature Dependencies

- Balance settlement and transfer behavior must remain authoritative in this repository.
- `craftalism-market` is an out-of-repo consumer and must not be changed here.

## Public Contract Change Rules

Changes to APIs, DTOs, schemas, persistence, rejection semantics, or external behavior require explicit card scope.

## Persistence Rules

Pressure-ladder persistence changes must be deterministic, auditable, and migration-safe. Legacy segment data may remain for migration or audit purposes until a selected card scopes its removal.

## Error and Failure Rules

- Reject hard pressure-bound violations with `INSUFFICIENT_STOCK`.
- Preserve stale quote and quote expiration semantics.
- Preserve no-mutation behavior for failed settlement.
- Preserve single-use quote lifecycle.

## Security and Permission Rules

Do not weaken authentication, authorization, idempotency, transfer safety, or incident handling while changing market behavior.

## Validation Rules

Use the validation command listed by the selected card. Prefer focused tests for narrow cards and `rtk ./gradlew test` from `java/` when the card changes cross-service market behavior.

## Source Areas

- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/`
- `java/src/main/resources/db/migration/`

## Test Areas

- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/`
