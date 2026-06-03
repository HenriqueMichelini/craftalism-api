# CARD-031: Derive Template Effect Direction

## Status

planned

## Objective

Change dashboard market event template create and update requests so the API derives `effectDirection` from authored effect basis-point ranges instead of requiring it as an authored request field.

## Context

Dashboard template authoring currently duplicates effect semantics by submitting both `minEffectBasisPoints`/`maxEffectBasisPoints` and `effectDirection`. The numeric range already determines whether a template raises prices, lowers prices, or represents a neutral blocking template.

## Required Reading

- `../contract.md`

## Expected Behavior

Template create and update requests omit `effectDirection`. The API derives `UP` when the effect range is entirely above `10000`, derives `DOWN` when the range is entirely below `10000`, and derives `BLOCK` only when both bounds are exactly `10000`. Responses continue to return the persisted `effectDirection` for display. Existing validation remains authoritative for invalid ranges and neutral blocking constraints.

## Acceptance Criteria

- [ ] `effectDirection` is no longer part of dashboard template create or update request DTOs.
- [ ] Template create and update persistence derive and store `effectDirection` from `minEffectBasisPoints` and `maxEffectBasisPoints`.
- [ ] Mixed or neutral non-blocking ranges are rejected by API validation.
- [ ] Dashboard template responses still include `effectDirection`.
- [ ] The market-events contract documents request omission and server-side derivation.
- [ ] Focused tests cover omitted request direction and derived response direction.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketEventTemplateCreateRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketEventTemplateUpdateRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventTemplateApiIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateTest.java
```

## Constraints

- Do not change unrelated behavior.
- Do not modify unrelated features.
- Do not introduce architectural changes unless explicitly required.
- Do not remove persisted or response `effectDirection`.
- Do not change scheduler, lifecycle, pricing, or dashboard frontend behavior.

## Validation Commands

```bash
./java/gradlew -p java test --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventTemplateApiIntegrationTest
```

If targeted tests cannot run, use:

```bash
./java/gradlew -p java test
```

## Out of Scope

- Removing `effectDirection` from persisted template rows.
- Removing `effectDirection` from template response DTOs.
- Changing active event, scheduler, pricing, or blocking runtime behavior.
- Dashboard frontend changes.

## Completion Notes

- Removed authored `effectDirection` from dashboard template create and update request DTOs.
- Derived and persisted `effectDirection` from effect basis-point ranges during create and update validation.
- Documented server-side derivation in the market-events contract while keeping responses unchanged.
- Added focused service coverage for derived `BLOCK` and mixed-range rejection, and updated dashboard API tests to omit request `effectDirection`.
- Validation passed: `./java/gradlew -p java test --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventTemplateApiIntegrationTest`.
