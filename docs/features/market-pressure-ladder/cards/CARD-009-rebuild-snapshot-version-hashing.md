# CARD-009: Rebuild SnapshotVersion Hashing

## Status

completed

## Objective

Hash only authoritative pressure state and trade-affecting config.

## Context

Source: audit finding 8.

`craftalism-api` owns stale detection semantics. Clients treat `snapshotVersion` as opaque.

## Required Reading

- `../contract.md`

## Expected Behavior

`snapshotVersion` changes when authoritative pressure state or trade-affecting config changes, and remains stable for derived-only recalculation.

## Acceptance Criteria

- [x] Hash includes required pressure config, `netPosition`, bounds, blocked/operating, and deterministic regen boundary.
- [x] Hash does not include persisted derived projections.
- [x] Hash does not include segment rows or virtual segment lists.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/test/...
```

## Constraints

- Do not expose hash internals to clients.
- Do not make the token parseable.
- Preserve stale quote rejection semantics.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Include unit tests proving version changes for authoritative state/config changes and stays stable for derived-only recalculation.

## Out of Scope

- Client-visible hash format documentation.
- Parseable token format.

## Suggested Commit Message

`feat(craftalism-api): hash pressure market snapshot state`

## Completion Notes

Implemented in `MarketSnapshotProjector` by hashing item identity, currency, authoritative pressure config, `netPosition`, configured pressure bounds, blocked/operating flags, and `lastUpdatedAt` as the deterministic regeneration boundary. Persisted derived projections and legacy segment row projections are no longer included in the hash payload.

Added focused unit coverage in `MarketSnapshotProjectorTest` for authoritative state/config changes and derived/segment-only stability.
