# CARD-025: Refresh Eligible Drift On Dashboard Market Item Reads

## Status

planned

## Objective

Refresh eligible persisted market drift before returning dashboard market-item rows so the admin view does not appear permanently neutral after a drift reset.

## Context

`POST /api/dashboard/market/drift/reset` correctly resets each item to neutral drift and starts a new hourly drift interval. Public snapshot and quote paths later evaluate eligible drift through `MarketSnapshotStateLoader`, including balanced items where `netPosition == 0`.

The dashboard refreshes its Market Items rows through `GET /api/dashboard/market/items`. That read currently loads persisted items directly and recomputes projections without invoking the shared lazy market-state refresh path. If there is no public snapshot or quote traffic after a reset, dashboard rows remain neutral indefinitely even after drift becomes eligible again.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../dashboard-crud-api/contract.md`

## Expected Behavior

Dashboard market-item listing refreshes eligible persisted market state through the existing lazy refresh behavior before returning rows. After an admin drift reset, the rows remain neutral until the normal drift interval elapses, then a dashboard item read can advance drift revision, persist the refreshed drift state, and return recomputed projections. Public snapshot, quote, execute, reset, named-event, and scheduler behavior remain unchanged.

## Acceptance Criteria

- [ ] `GET /api/dashboard/market/items` uses the existing lazy market-state refresh behavior before mapping dashboard rows.
- [ ] Eligible drift is evaluated and persisted for balanced items where `netPosition == 0`.
- [ ] Dashboard reads do not evaluate drift before the normal interval elapses.
- [ ] Dashboard reads return projections recomputed from the refreshed drift state.
- [ ] Dashboard reads do not introduce a separate drift algorithm or alter pressure regeneration semantics.
- [ ] Existing public snapshot, quote, execute, reset, named-event, scheduler, and dashboard mutation behavior remains unchanged.
- [ ] Tests cover dashboard read behavior before and after an eligible post-reset drift interval.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/DashboardMarketItemService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/query/MarketSnapshotStateLoader.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/configuration/MarketServiceConfiguration.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketItemCrudApiIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/application/query/MarketSnapshotStateLoaderTest.java
```

## Constraints

- Reuse the existing lazy market-state refresh path.
- Do not duplicate drift evaluation logic in dashboard services.
- Do not change drift cadence, bounds, deterministic movement, or mean reversion.
- Do not add a scheduled drift worker.
- Do not change public or dashboard route shapes.
- Do not change authorization rules.
- Do not change named event lifecycle, scheduler, or template behavior.
- Do not change pressure-ladder pricing rules.

## Validation Commands

Run from `java/`.

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotStateLoaderTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

## Out of Scope

- Frontend changes.
- Timed or scheduled drift evaluation.
- Drift algorithm retuning.
- Reset route or response changes.
- Public drift history or audit APIs.
- Per-item or per-category reset controls.

## Completion Notes

- `DashboardMarketItemService` now loads rows through the shared lazy market-state refresh path before recomputing dashboard projections.
- `MarketSnapshotStateLoader` is Spring-managed and shared by public market reads and dashboard market-item reads.
- Added dashboard integration coverage proving that a post-reset read stays neutral before the drift interval and persists refreshed balanced-item drift after the interval elapses.
- Validation passed:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotStateLoaderTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```
