# CARD-004: Implement Pressure Price Derivation

## Status

planned

## Objective

Introduce deterministic pressure segment and unit price derivation.

## Context

Source: audit finding 4.

`craftalism-api` owns authoritative market pricing.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

The backend can derive market segment and unit price from a pressure position using the documented bounded curve.

## Acceptance Criteria

- [ ] Uses `Math.floorDiv(netPosition, segmentSize)` for segment derivation.
- [ ] Segment `0` prices exactly at `baseUnitPrice`.
- [ ] Positive pressure approaches `maxUnitPrice`.
- [ ] Negative pressure approaches `minUnitPrice`.
- [ ] Unit price is rounded and clamped within bounds.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java
java/src/test/...
```

Or a new package-private pricing helper if that better fits the existing service shape.

## Constraints

- Do not wire quote planning yet unless necessary.
- Do not change DTOs.
- Explicitly test negative floor-division behavior.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Prefer focused unit tests for positive, zero, negative, and boundary positions when available.

## Out of Scope

- Quote planning traversal.
- DTO changes.

## Suggested Commit Message

`feat(craftalism-api): derive pressure ladder prices`

## Completion Notes

