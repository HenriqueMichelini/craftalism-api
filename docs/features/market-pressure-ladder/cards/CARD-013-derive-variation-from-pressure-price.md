# CARD-013: Derive Variation From Pressure Price

## Status

completed

## Objective

Make market `variationPercent` reflect actual pressure-derived price movement instead of fixed per-trade increments.

## Context

Source: triage finding for `iron_ingot` buys where `variationPercent` increased while the item price stayed unchanged inside segment `0`.

`craftalism-api` owns authoritative market pricing, derived projections, execution mutation, and snapshot semantics. The pressure-ladder source of truth defines `variationPercent` as price movement from `baseUnitPrice`.

The current runtime mutates `variationPercent` by a fixed `0.6` per successful BUY and `-0.6` per successful SELL. This can make snapshots report variation movement even when `buyUnitEstimate` remains at the base price because pressure has not crossed a segment boundary.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Snapshot and execute success item payloads expose `variationPercent` derived from the current pressure-derived buy price relative to `baseUnitPrice`.

When `netPosition` remains in segment `0`, `buyUnitEstimate` equals `baseUnitPrice` and `variationPercent` is `0`.

When BUY pressure crosses into a positive segment, `variationPercent` increases with the pressure-derived buy price. When SELL pressure moves into negative pressure, `variationPercent` decreases with the pressure-derived buy price.

## Acceptance Criteria

- [ ] `variationPercent` is recomputed from pressure-derived price movement and no longer changes by a fixed per-trade delta.
- [ ] Segment `0` snapshots report `variationPercent` as `0` when `buyUnitEstimate == baseUnitPrice`.
- [ ] Positive pressure segments report positive `variationPercent` based on `buyUnitEstimate` versus `baseUnitPrice`.
- [ ] Negative pressure segments report negative `variationPercent` based on `buyUnitEstimate` versus `baseUnitPrice`.
- [ ] Successful BUY and SELL execution still mutate only `netPosition`, recompute derived projections, and preserve quote verification and balance settlement behavior.
- [ ] Failed settlement still leaves market pressure and derived market state unchanged.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlannerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjectorTest.java
```

Add controller integration coverage only if service-level tests do not cover the execute success `updatedItem` payload.

## Constraints

- This card explicitly scopes the public snapshot semantic correction for `variationPercent`; do not change the DTO field name or type.
- Do not change quote request, quote response, execute request, or rejection payload shape.
- Do not change price curve, segment derivation, quote traversal, pressure mutation, regeneration, balance settlement, or stale quote semantics.
- Do not add or modify database schema.
- Do not implement client behavior in this repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest
```

Run from `java/`.

If controller integration coverage is changed, also run:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

## Out of Scope

- Changing pressure-ladder pricing math.
- Changing `segmentSize` or catalog defaults for `iron_ingot`.
- Client-side display changes in `craftalism-market`.
- Removing legacy variation persistence columns.
- Broad documentation rewrites.

## Suggested Commit Message

`fix(craftalism-api): derive market variation from pressure price`

## Completion Notes

- Derived `variationPercent` from pressure-derived `buyUnitEstimate` versus `baseUnitPrice` during market projection recomputation.
- Removed fixed BUY/SELL variation deltas from trade execution while preserving pressure mutation and settlement flow.
- Added service coverage for segment `0`, positive pressure, negative pressure, execute success, and failed settlement no-mutation behavior.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest` from `java/` passed.
