# CARD-028: Validate Admin Event Effect Range

## Status

completed

## Objective

Reject admin-created or admin-updated market events whose `effectBasisPoints` falls outside the selected template's allowed effect range.

## Context

Triage confirmed that admin event requests can persist any positive `effectBasisPoints` value. The pricing pipeline treats this value as a direct multiplier where `10000` is neutral, so an out-of-range value such as `1000` becomes a 10% price multiplier and then clamps eligible items to their minimum unit price.

## Required Reading

- `../contract.md`

## Expected Behavior

Admin start, supersede, and update paths validate explicit `effectBasisPoints` against the event template's `minEffectBasisPoints` and `maxEffectBasisPoints` before persisting or activating the event. Invalid values fail with the existing market event template validation error path and do not mutate event state.

## Acceptance Criteria

- [ ] Admin manual start rejects an explicit `effectBasisPoints` below the selected template's minimum.
- [ ] Admin manual start rejects an explicit `effectBasisPoints` above the selected template's maximum.
- [ ] Admin supersede rejects an invalid explicit `effectBasisPoints` before ending the currently active event.
- [ ] Admin update rejects an invalid `effectBasisPoints` before mutating the target event or incrementing `effectVersion`.
- [ ] Omitted `effectBasisPoints` still defaults to the selected template's minimum effect.
- [ ] Existing scheduler-selected template ranges and pricing formulas remain unchanged.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventAdminService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventAdminServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventAdminApiIntegrationTest.java
```

## Constraints

- Do not reinterpret `effectBasisPoints`; `10000` remains the neutral multiplier basis.
- Do not change pricing formula, drift behavior, scheduler selection, persistence schema, or public market responses.
- Do not implement client UI or dashboard/BFF conversion behavior.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventAdminServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest
```

Run from `java/`.

## Out of Scope

- Changing the meaning or naming of `effectBasisPoints`.
- Adding new DTO fields for percentage deltas.
- Modifying market snapshot, quote, execute, drift, or scheduler pricing behavior.
- Changing dashboard or BFF request conversion.

## Completion Notes

- Added template-range validation for admin manual start, supersede, and update event paths.
- Ensured invalid supersede requests are rejected before ending the currently active event.
- Added service and API regression coverage for below-range, above-range, defaulted, supersede, and update validation behavior.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventAdminServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest` passed from `java/`.
