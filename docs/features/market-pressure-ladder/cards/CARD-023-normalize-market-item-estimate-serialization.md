# CARD-023: Normalize Market Item Estimate Serialization

## Status

completed

## Objective

Ensure every backend market item API response serializes buy and sell estimates from current pressure-derived projections so `sellUnitEstimate < buyUnitEstimate`.

## Context

CARD-022 moved the display spread rule into `MarketTradePlanner.recomputeDerivedProjections`, and snapshot and execute success item projections call that recomputation before serialization. Dashboard market item list responses still return repository entities through `MarketItemMapper`, which reads persisted estimate fields directly and can expose stale or equal estimates after legacy data, direct persistence setup, or older rows.

This card keeps the display spread as a projection rule and closes the remaining backend-owned serialization boundary.

## Required Reading

- `../contract.md`

## Expected Behavior

All backend market item response DTOs that expose `buyUnitEstimate` and `sellUnitEstimate` must serialize current derived projection values. Dashboard market item list, create, and update responses must expose `sellUnitEstimate < buyUnitEstimate` without changing quote totals, executed unit prices, pressure mutation, snapshot version semantics, persistence schema, or DTO field names.

## Acceptance Criteria

- [x] Dashboard market item list responses recompute or project derived estimates before mapping to `MarketItemResponseDTO`.
- [x] Dashboard market item create and update responses continue to expose derived estimates after mutation.
- [x] Market execute success controller integration coverage asserts `updatedItem.sellUnitEstimate < updatedItem.buyUnitEstimate` after a BUY inside segment `0`.
- [x] Dashboard controller integration coverage proves stale persisted equal estimates are normalized in the response.
- [x] Quote totals and executed unit prices still use pressure-position traversal and remain unchanged.
- [x] No database schema or DTO field names are changed.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DashboardMarketItemService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketItemCrudApiIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
docs/features/market-pressure-ladder/cards/CARD-023-normalize-market-item-estimate-serialization.md
```

## Constraints

- Do not change quote planning, quote response totals, or executed unit prices.
- Do not change pressure mutation direction, regeneration, stale quote behavior, rate limiting, settlement behavior, or snapshot version hashing.
- Do not add or modify database schema.
- Do not change DTO field names or types.
- Do not implement client behavior in this repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

If the focused controller validation passes, run:

```bash
rtk ./gradlew clean build
```

Run from `java/`.

## Out of Scope

- Client-side display changes in `craftalism-market`.
- Changing the pressure pricing model.
- Changing quote or execute request/response shapes.
- Enforcing the projection spread with database constraints.
- Backfilling existing persisted estimate columns outside normal application projection paths.

## Completion Notes

- Dashboard market item list responses now recompute pressure-derived projections before mapping to `MarketItemResponseDTO`, preventing stale persisted equal estimates from leaking through the dashboard API.
- Dashboard controller coverage now saves an item with stale equal persisted estimates and verifies the list response normalizes them to `sellUnitEstimate < buyUnitEstimate`.
- Market contract integration coverage now asserts BUY execute success `updatedItem` exposes `buyUnitEstimate` and `sellUnitEstimate` with the expected segment `0` display spread.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest` from `java/` passed.
- Validation: `rtk ./gradlew clean build` from `java/` passed after rerunning with approved access to the Gradle wrapper cache under `~/.gradle`.
