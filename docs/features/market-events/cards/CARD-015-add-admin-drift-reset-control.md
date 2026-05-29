# CARD-015: Add Admin Drift Reset Control

## Status

planned

## Objective

Add a guarded dashboard/admin operation to reset or re-seed persisted market drift state after bad tuning or operational drift-state incidents.

## Context

Live local evidence showed most persisted item drift multipliers saturated at the exact absolute bounds after six drift revisions. Retuning future drift evaluation prevents recurrence, but existing stored drift state can remain pinned until enough future evaluations move it away from the bounds.

This card explicitly scopes a dashboard/admin backend control because it changes external admin behavior.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

An authorized market admin can reset persisted drift state for all market items, returning drift multipliers to neutral and advancing or preserving audit-relevant drift metadata in a deterministic way. Public market endpoints remain unchanged except that subsequent snapshots reflect the reset drift state through existing price and `variationPercent` fields.

## Acceptance Criteria

- [ ] A dashboard/admin route exists for resetting all market item drift state.
- [ ] The route requires the existing market admin authority used for event-admin operations.
- [ ] Reset sets each item drift multiplier to neutral `10000` basis points.
- [ ] Reset updates drift metadata consistently so snapshot versions change after the reset.
- [ ] Reset recomputes derived buy/sell estimates and `variationPercent` for affected items.
- [ ] Existing public market snapshot, quote, execute, and named event behavior is not broadened or otherwise changed.
- [ ] Tests cover authorized reset, rejected non-admin reset, persisted drift state changes, and snapshot-visible recomputation.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not expose drift reset on public `/api/market/**` routes.
- Do not weaken existing event-admin authorization.
- Do not change named event lifecycle, scheduler, or template behavior.
- Do not change pressure-ladder pricing rules.
- Do not add automatic drift resets.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminSecurityTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketReadServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

## Out of Scope

- Retuning the drift algorithm.
- Frontend controls for invoking the reset.
- Public drift history or audit log APIs.
- Per-item or per-category reset selection.

## Completion Notes

Implemented. Added a dashboard-only drift reset route guarded by the existing
`SCOPE_market:admin` authority. Reset returns all persisted item drift
multipliers to neutral `10000`, advances drift metadata, recomputes derived
estimates and `variationPercent`, and is covered by admin, non-admin, persisted
state, and snapshot-visible recomputation tests.
