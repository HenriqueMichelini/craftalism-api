# CARD-007: Implement Pressure Regeneration

## Status

planned

## Objective

Replace stock restoration regeneration with deterministic pressure recovery toward zero.

## Context

Source: audit finding 6.

`craftalism-api` owns regeneration and stale detection state.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Regeneration moves pressure toward equilibrium and preserves deterministic tick accounting.

## Acceptance Criteria

- [ ] Positive `netPosition` decreases toward `0`.
- [ ] Negative `netPosition` increases toward `0`.
- [ ] `lastUpdatedAt` advances only by whole applied ticks.
- [ ] Fractional tick remainder is preserved.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketReadService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java
java/src/test/...
```

## Constraints

- Do not change quote or execute endpoint shapes.
- Do not implement client behavior.
- Preserve `snapshotVersion` and stale detection semantics unless explicitly scoped by the implementation.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Include unit and integration tests for positive, negative, zero pressure, no tick, and multiple ticks.

## Out of Scope

- Endpoint shape changes.
- Client behavior.

## Suggested Commit Message

`feat(craftalism-api): regenerate market pressure toward equilibrium`

## Completion Notes

