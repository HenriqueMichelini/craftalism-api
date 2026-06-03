# CARD-029: Add Dashboard Market Event Template Update API

## Status

completed

## Objective

Expose a dashboard API endpoint to update an existing authored market event template.

## Context

The dashboard can list and create market event templates, but operators cannot edit an existing template. `DashboardMarketEventTemplateController` currently exposes only `GET /api/dashboard/market/event-templates` and `POST /api/dashboard/market/event-templates`, and `MarketEventTemplateService` has list/create behavior only.

This repository owns authoritative backend template validation, persistence, route semantics, DTOs, and authorization. The dashboard must consume the confirmed route rather than define edit behavior locally.

## Required Reading

- `../contract.md`
- `CARD-022-add-dashboard-market-event-template-api.md`
- `CARD-023-extract-default-event-template-catalog.md`

## Expected Behavior

Authenticated market admins can update an existing persisted market event template through a dashboard/admin endpoint. The update path validates the authored template configuration with the same MVP invariants as create, preserves API-owned persistence semantics, returns the updated template row, and keeps template administration internal to the `SCOPE_market:admin` boundary.

## Acceptance Criteria

- [ ] A dashboard/admin update route exists for persisted market event templates, using the existing `/api/dashboard/market/event-templates` route family.
- [ ] The update route requires `SCOPE_market:admin`; generic `SCOPE_api:write` cannot access it.
- [ ] The update request shape is explicit and does not rely on partial, ambiguous, or silently ignored fields.
- [ ] `templateId` identity behavior is explicit: either path-bound and immutable, or deliberately changeable with validated uniqueness semantics.
- [ ] Updating an unknown template returns a structured validation-style client error without creating a new template.
- [ ] Invalid authored template configurations return validation problems without mutating the stored template.
- [ ] A successful update persists the authored fields, refreshes `updatedAt`, preserves the original `createdAt`, and returns the API-updated template row.
- [ ] Existing list and create template behavior remains unchanged.
- [ ] Existing event instance lifecycle, scheduler, pricing, quote, execute, drift, and blocking behavior remains unchanged except for later consumers observing the persisted template values.
- [ ] Integration, security, and focused service coverage verify successful update, unknown-template rejection, invalid-template rejection, timestamp behavior, authorization, and create/list regressions.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/SecurityConfig.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventTemplateController.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventTemplateApiIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventAdminSecurityTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateTest.java
```

## Constraints

- Preserve the hand-authored template model.
- Keep template administration internal to the dashboard admin boundary.
- Do not add template delete behavior.
- Do not change persistence schema unless the update contract explicitly requires it.
- Do not change event instance lifecycle behavior.
- Do not change scheduler selection, pricing pipeline, quote, execute, drift, or blocking formulas.
- Do not implement frontend table or modal behavior in this backend repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventTemplateApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminSecurityTest --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateTest
```

Run from `java/`.

Fallback for unexpected focused-test filtering issues:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Updating dashboard frontend client, table, or modal behavior.
- Deleting persisted templates.
- Template preview or scheduler simulation endpoints.
- Persistence migrations unless required by an explicitly chosen update contract.
- Public market API changes.
- Scheduler, event lifecycle, pricing, quote, execute, drift, or blocking behavior changes.

## Completion Notes

- Added `PUT /api/dashboard/market/event-templates/{templateId}` with
  path-bound immutable `templateId`.
- Added an explicit update request DTO that reuses authored template fields
  except identity.
- Updated `MarketEventTemplateService` to reject unknown templates, validate
  update requests with the same MVP invariants as create, preserve `createdAt`,
  refresh `updatedAt`, and return the persisted row.
- Extended integration, security, and service tests for successful update,
  unknown-template rejection, invalid-update no-mutation behavior, timestamp
  behavior, authorization, and list/create regression coverage.
- Validation: `rtk ./gradlew test --tests
  io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventTemplateApiIntegrationTest
  --tests
  io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminSecurityTest
  --tests
  io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateTest`
  passed from `java/`.
