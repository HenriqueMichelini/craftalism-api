# CARD-022: Add Dashboard Market Event Template API

## Status

completed

## Objective

Expose dashboard API endpoints to list and create authored market event templates.

## Context

Market event templates are persisted and seeded, but the dashboard API only
exposes event instances. Dashboard users need an internal template list for
visualization and a validated create path for authoring new templates.

## Required Reading

- `../contract.md`

## Expected Behavior

Authenticated market admins can list persisted event templates and create new
templates through dashboard API endpoints. Template creation validates the MVP
template invariants before persistence.

## Acceptance Criteria

- [ ] `GET /api/dashboard/market/event-templates` returns persisted templates.
- [ ] `POST /api/dashboard/market/event-templates` creates a new template and
  returns `201 Created`.
- [ ] Duplicate template IDs and invalid authored template configurations return
  validation problems without persisting a template.
- [ ] Template routes require `SCOPE_market:admin`; generic `SCOPE_api:write`
  cannot access them.
- [ ] Existing event instance admin behavior remains unchanged.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/SecurityConfig.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventTemplateService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
```

## Constraints

- Preserve the hand-authored template model.
- Keep template administration internal to the dashboard admin boundary.
- Do not change event instance lifecycle behavior.
- Do not change persistence schema.
- Do not implement frontend table or form rendering in this backend repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventTemplateApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminSecurityTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTest
```

## Out of Scope

- Updating or deleting persisted templates.
- Frontend dashboard table and form implementation.
- Scheduler, event lifecycle, or pricing behavior changes.
- Persistence migrations.

## Completion Notes

- Added authenticated dashboard endpoints to list and create market event
  templates.
- Added template create validation for duplicate IDs, duration and effect ranges,
  automatic-selection restrictions, effect direction rules, blocking-template
  restrictions, and JSON target metadata.
- Added integration and security coverage for template API behavior.
- Validation: the card validation command passed from `java/`.
- Regression validation: `rtk ./gradlew test` passed from `java/`.
