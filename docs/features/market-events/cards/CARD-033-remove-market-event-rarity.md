# CARD-033: Remove Market Event Rarity

## Status

completed

## Objective

Remove market event rarity from the API contract, Java domain model, DTOs, persistence schema, migrations, and tests.

## Context

Run this card after `CARD-032` so scheduler and validation behavior no longer depend on rarity. The dashboard will consume the rarity-free API contract through its own `CARD-019` through `CARD-022` after this API-side work is confirmed.

## Required Reading

- `../contract.md`
- `CARD-032-retire-rarity-dependent-event-rules.md`

## Expected Behavior

Market event templates and instances have no rarity field anywhere in the authoritative backend contract. Template create and update requests do not accept rarity. Template responses and admin event responses do not return rarity. Persistence has no rarity columns for templates or instances. The `MarketEventRarity` enum is removed, and no code derives or fabricates a rarity value from `automaticWeight` or any other field.

## Acceptance Criteria

- [ ] `MarketEventRarity` is deleted.
- [ ] `MarketEventTemplate` and `MarketEventInstance` no longer define rarity fields, getters, setters, imports, or ORM mappings.
- [ ] Dashboard template create, update, and response DTOs omit rarity.
- [ ] Admin event response DTOs omit rarity.
- [ ] Template create/update service mapping no longer reads or writes rarity.
- [ ] Admin and scheduler event creation no longer copies rarity from templates to instances.
- [ ] Default template catalog and template builder no longer set rarity.
- [ ] Market event migrations and migration tests create/use template and instance tables without rarity columns.
- [ ] `docs/features/market-events/contract.md` documents named events and template APIs without rarity, and states that scheduler behavior uses explicit fields rather than bucket-like rarity.
- [ ] `rg -n "MarketEventRarity|\\brarity\\b|Rarity" java/src/main java/src/test docs/features/market-events/contract.md` has no remaining hits except intentionally retained historical completed-card text outside this card scope.
- [ ] Focused API, migration, scheduler, pricing, lifecycle, and admin tests are updated to build events/templates without rarity.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketEventAdminResponseDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketEventTemplateCreateRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketEventTemplateResponseDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketEventTemplateUpdateRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventAdminService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/domain/event/DefaultMarketEventTemplateCatalog.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/domain/event/MarketEventTemplateBuilder.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/scheduling/MarketEventScheduler.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketEventInstance.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketEventRarity.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketEventTemplate.java
java/src/main/resources/db/migration/V22__create_market_events_tables.sql
java/src/main/resources/db/migration/V25__drop_market_event_rarity.sql
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/MarketEventMigrationTest.java
```

## Constraints

- Do not replace rarity with another bucket-like field.
- Do not derive or fabricate rarity from `automaticWeight`, effect ranges, scope, cooldown, or blocking rules.
- Preserve authoritative behavior for pricing, lifecycle status, active event locking, target selection, cooldowns, and effect basis-point validation.
- Keep dashboard/admin authorization unchanged.
- Do not implement dashboard frontend changes.
- Do not perform unrelated package moves or refactors.

## Validation Commands

Run from `java/`:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketEventMigrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventTemplateApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests '*MarketEvent*Test'
```

Then run:

```bash
rtk ./gradlew test
```

## Out of Scope

- Dashboard frontend contract consumption.
- Adding a new rarity replacement, tier, bucket, label, or probability class.
- Changing market pressure-ladder pricing rules.
- Changing player-facing event snapshot disclosure beyond the absence of rarity.

## Suggested Commit Message

`refactor(market-events): remove market event rarity`

## Completion Notes

- Removed market event rarity from domain models, DTOs, service mappings, default templates, scheduler/admin event creation, tests, and the market-events contract.
- Updated V22 clean-schema migration to create rarity-free market event tables and added V25 to drop existing rarity columns.
- Validated with the focused CARD-033 command and `rtk ./gradlew test`.
- The only remaining literal `rarity` hits under `java/src/main` are in `V25__drop_market_event_rarity.sql`, where the existing column name is required to migrate already-deployed schemas.
