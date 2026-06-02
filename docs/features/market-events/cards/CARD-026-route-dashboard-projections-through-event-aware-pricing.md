# CARD-026: Route Dashboard Projections Through Event-Aware Pricing

## Status

implemented

## Objective

Route dashboard market-item and drift-reset projection recomputation through the existing event-aware market pricing path so eligible active named-event modifiers are reflected consistently.

## Context

`GET /api/dashboard/market/items` loads rows through the shared lazy market-state refresh path, but `DashboardMarketItemService` then recomputes every row with a private `new MarketTradePlanner()`. The no-argument planner delegates to `this(null)`, so named-event pricing falls back to a neutral multiplier and overwrites event-aware projections before the dashboard response is mapped.

The same private neutral planner is used when dashboard market-item create and update responses recompute derived projections. `MarketDriftAdminService` also uses a private neutral planner when reset persists recomputed derived state.

Public snapshot, quote, and execute wiring already constructs event-aware planners with `MarketEventPricingService`. This is implementation drift against the existing market-events contract, not a contract change.

`MarketEventPricingService` caches the effective active event in a `ThreadLocal`. Dashboard/admin batch recomputation must use an explicit cache lifecycle so all rows within one operation observe a consistent effective event and later operations do not reuse stale event state after lifecycle changes.

## Dependencies

- `CARD-004-apply-active-event-modifiers-to-market-pricing.md`
- `CARD-015-add-admin-drift-reset-control.md`
- `CARD-025-refresh-eligible-drift-on-dashboard-market-item-reads.md`

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../dashboard-crud-api/contract.md`

## Expected Behavior

Dashboard market-item list, create, and update projections use the existing shared pricing pipeline in this order: pressure, drift, active named-event modifier when eligible, clamp, then sell derivation. Drift reset still neutralizes persisted drift state, but its recomputed persisted projections use the same event-aware path. Dashboard/admin projection operations establish and clear the named-event pricing request cache at operation boundaries.

## Acceptance Criteria

- [ ] Spring wiring supplies event-aware `MarketTradePlanner` usage for public market paths, `MarketSnapshotStateLoader`, `DashboardMarketItemService`, and `MarketDriftAdminService`; dashboard/admin services no longer construct neutral planners internally.
- [ ] `GET /api/dashboard/market/items` returns event-inclusive `buyUnitEstimate`, `sellUnitEstimate`, and `variationPercent` for an item eligible for the currently effective active named event.
- [ ] Dashboard market-item create and update responses return event-inclusive `buyUnitEstimate`, `sellUnitEstimate`, and `variationPercent` when the created or updated item is eligible for the currently effective active named event.
- [ ] Drift reset still persists neutral drift basis points and advances drift metadata, while persisted derived projections reflect any currently effective active named-event modifier for eligible items.
- [ ] Dashboard/admin multi-row recomputation clears the pricing request cache at operation boundaries so rows within one operation use one effective-event view and a later operation observes event start, end, cancellation, or supersession without stale `ThreadLocal` state.
- [ ] Ineligible items and no-active-event cases preserve pressure-plus-drift pricing behavior.
- [ ] `GET /api/dashboard/market/items` continues to use the shared lazy market-state refresh path introduced by `CARD-025`.
- [ ] Public snapshot, quote, execute, event lifecycle, scheduler, drift cadence, pressure pricing, API schemas, and dashboard frontend behavior remain unchanged.
- [ ] Focused regression tests cover eligible active-event list projections, ineligible or no-active-event list projections, eligible create/update response projections, reset-side persisted projections during an active event, and cache freshness across dashboard/admin operations.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/configuration/MarketServiceConfiguration.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/query/MarketSnapshotStateLoader.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/DashboardMarketItemService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketDriftAdminService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketItemCrudApiIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventAdminApiIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/application/query/MarketSnapshotStateLoaderTest.java
```

## Constraints

- Reuse `MarketTradePlanner`, `MarketPricingPipeline`, and `MarketEventPricingService`; do not add a dashboard-specific pricing formula.
- Preserve pricing order: pressure, drift, active named event modifier, clamp, sell derivation.
- Preserve the dashboard lazy drift refresh path and do not duplicate drift evaluation logic.
- Do not change public or dashboard route shapes, DTO fields, persistence schemas, or authorization rules.
- Do not change public snapshot, quote, or quote-backed execute semantics.
- Do not change event lifecycle, scheduler behavior, drift cadence, drift tuning, pressure regeneration, pressure mutation direction, or sell percentage semantics.
- Do not modify dashboard frontend code.

## Validation Commands

Run from `java/`.

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotStateLoaderTest --tests io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Fallback:

```bash
rtk ./gradlew test
```

## Out of Scope

- Dashboard frontend changes.
- New API fields or routes.
- Named-event contract changes.
- Public snapshot, quote, or execute behavior changes.
- Event lifecycle, scheduler, template, or authorization changes.
- Drift algorithm retuning, cadence changes, or scheduled drift evaluation.
- Pressure-ladder formula changes.
- Persistence migrations.

## Completion Notes

- Added one Spring-managed event-aware `MarketTradePlanner` shared by public market, snapshot-state refresh, dashboard market-item, and drift-reset paths.
- Added explicit named-event pricing cache boundaries around snapshot-state refresh, dashboard list/create/update projection recomputation, and drift reset.
- Added regression coverage for eligible and ineligible dashboard list projections, dashboard create/update projections, drift-reset persisted projections during an active event, and cache freshness between dashboard/admin operations.
- Validation passed:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotStateLoaderTest --tests io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
rtk ./gradlew test
```
