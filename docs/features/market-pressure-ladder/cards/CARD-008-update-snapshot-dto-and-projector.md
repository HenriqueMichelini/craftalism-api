# CARD-008: Update Snapshot DTO And Projector

## Status

planned

## Objective

Expose pressure snapshot fields and remove target-contract `currentStock`.

## Context

Source: audit finding 7.

`craftalism-api` owns the canonical market snapshot contract. `craftalism-market` consumes this API shape.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Snapshot and execute success item payloads expose the pressure item shape required by the pressure-ladder contract.

## Acceptance Criteria

- [ ] Snapshot item exposes `marketPressure`, `marketSegment`, and `pressureMagnitude`.
- [ ] Target snapshot no longer exposes `currentStock`.
- [ ] Execute success `updatedItem` uses the same pressure item shape.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketSnapshotItemDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketExecuteSuccessResponseDTO.java
java/src/test/...
```

## Constraints

- Do not add a client compatibility adapter unless explicitly requested.
- Do not change quote request or response shape.
- This is a breaking API contract change for old clients and must remain explicitly scoped to this card.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Include controller integration tests for snapshot and execute response.

## Out of Scope

- Client compatibility adapter.
- Quote request or response changes.

## Suggested Commit Message

`feat(craftalism-api): expose pressure market snapshots`

## Completion Notes

