# CARD-022: Enforce Buy Sell Estimate Spread

## Status

completed

## Objective

Make backend market projections always expose a sell estimate that is lower than the buy estimate.

## Context

Source: triage finding for buy operations where `buyUnitEstimate` and `sellUnitEstimate` remain equal after pressure changes inside segment `0`.

`craftalism-api` owns authoritative market pricing, derived projections, quote planning, execution mutation, and snapshot semantics. The current pressure-ladder contract prices the next buy at `netPosition` and the next sell at `netPosition - 1`; because segment `0` prices exactly at `baseUnitPrice`, small positive pressure values can expose equal buy and sell estimates.

This card explicitly changes the public snapshot and execute success item estimate semantics so display estimates always preserve a spread: `sellUnitEstimate < buyUnitEstimate`.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`
- `../../../craftalism-market-pressure-ladder-changelog.md`

## Expected Behavior

Snapshot and execute success item payloads always expose `sellUnitEstimate` lower than `buyUnitEstimate`.

Quote planning remains quantity-sensitive and pressure-position based. Successful BUY still increases `netPosition`; successful SELL still decreases `netPosition`. Hard pressure-bound, stale quote, quote lifecycle, settlement, regeneration, and snapshot version semantics remain unchanged except where estimate values change as part of the scoped public behavior update.

## Acceptance Criteria

- [ ] `docs/features/market-pressure-ladder/contract.md` states that projected sell estimates must be lower than projected buy estimates.
- [ ] `docs/market-pressure-ladder-sigmoid-pricing.md`, `docs/market-contract-mvp.md`, and `docs/craftalism-market-pressure-ladder-changelog.md` no longer document equal buy and sell estimates as valid implemented examples.
- [ ] `MarketTradePlanner.recomputeDerivedProjections` derives `sellUnitEstimate < buyUnitEstimate` for segment `0`, positive pressure, and negative pressure.
- [ ] The spread rule never produces a zero or negative sell estimate; configured `minUnitPrice` remains respected.
- [ ] Quote totals and executed unit prices still use the authoritative pressure traversal rules for BUY and SELL.
- [ ] Successful BUY and SELL execution still mutate pressure in the same direction and recompute derived projections.
- [ ] Failed settlement still leaves market pressure and derived market state unchanged.
- [ ] Focused tests cover equal-segment estimate cases, including small positive pressure after a buy.

## Expected Files to Change

```text
docs/features/market-pressure-ladder/contract.md
docs/market-pressure-ladder-sigmoid-pricing.md
docs/market-contract-mvp.md
docs/craftalism-market-pressure-ladder-changelog.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DashboardMarketItemService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItem.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlannerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjectorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketItemCrudApiIntegrationTest.java
```

Add controller integration coverage only if service-level tests do not cover snapshot and execute success payload projections.

## Constraints

- This card explicitly scopes the public snapshot and execute success item estimate behavior change.
- Do not change DTO field names or types.
- Do not change quote request, quote response, execute request, or rejection payload shape.
- Do not change pressure mutation direction, hard pressure-bound rejection semantics, quote lifecycle, balance settlement, regeneration, or snapshot version inputs except where estimate values are already part of the existing response projection.
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

- Client-side display changes in `craftalism-market`.
- Changing pressure mutation rules.
- Changing segment derivation or pressure traversal for quote totals.
- Changing catalog defaults, `segmentSize`, or price sensitivity.
- Removing legacy market segment persistence.

## Suggested Commit Message

`fix(craftalism-api): enforce market estimate spread`

## Completion Notes

- Updated the pressure-ladder contract and public market docs so snapshot and execute success item projections require `sellUnitEstimate < buyUnitEstimate`.
- Added spread enforcement to `MarketTradePlanner.recomputeDerivedProjections` without changing quote traversal, quote totals, executed unit prices, pressure mutation, settlement, or snapshot version inputs.
- Added admin and catalog validation so new market item configs must leave at least one whole unit for the display estimate spread.
- Added focused service coverage for segment `0`, small positive pressure, negative pressure, minimum-bound spread behavior, snapshot payloads, and execute recomputation.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest` from `java/` passed.
