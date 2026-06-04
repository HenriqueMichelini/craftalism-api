# CARD-034: Normalize Market Event Migration History

## Status

completed

## Objective

Restructure market event Flyway migrations so the clean migration chain creates the current rarity-free event schema directly.

## Context

Migration audit confirmed that the current event migration chain preserves a historical rarity implementation that the domain and codebase have since removed:

- `V22__create_market_events_tables.sql` creates `rarity` columns on templates and instances.
- `V25__drop_market_event_rarity.sql` drops those same columns after the domain model, DTOs, scheduler, admin paths, and contract became rarity-free.
- `MarketEventMigrationTest` still has an intermediate `V22` test that asserts the obsolete rarity columns exist.

The completed rarity-removal card kept a forward migration for already-applied databases. This card intentionally bypasses that historical Flyway preservation rule for a local/current-schema cleanup. Do not run this card against any database that has already applied the existing migration versions unless an explicit migration reset/repair plan is approved.

## Required Reading

- `../contract.md`
- `CARD-033-remove-market-event-rarity.md`
- `../../market-pressure-ladder/contract.md`

## Expected Behavior

Fresh migrations create market event template and instance tables without any rarity columns. Current event lifecycle, scheduler, admin controls, blocking behavior, pricing modifiers, quote context, and public snapshot event context remain unchanged.

## Acceptance Criteria

- [ ] `V22__create_market_events_tables.sql` creates rarity-free `market_event_templates` and `market_event_instances` tables.
- [ ] `V25__drop_market_event_rarity.sql` is removed or rewritten according to the migration version policy chosen by implementation.
- [ ] Migration tests no longer assert that any intermediate event table contains `rarity`.
- [ ] Full-chain migration tests still assert rarity is absent from event template and instance tables.
- [ ] `rg -n "MarketEventRarity|\\brarity\\b|Rarity" java/src/main java/src/test docs/features/market-events/contract.md` has no hits except intentionally retained historical completed-card text outside runtime/test scope.
- [ ] Active event uniqueness, target checks, event template fields, scheduler lock migration, and quote pricing context migrations still work after the cleanup.
- [ ] Event API, scheduler, lifecycle, pricing, blocking, admin authorization, and DTO behavior are unchanged.

## Expected Files to Change

```text
java/src/main/resources/db/migration/V22__create_market_events_tables.sql
java/src/main/resources/db/migration/V25__drop_market_event_rarity.sql
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/MarketEventMigrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
docs/features/market-events/cards/CARD-034-normalize-market-event-migration-history.md
```

## Constraints

- This is a migration-history cleanup only; do not change event domain behavior.
- Do not reintroduce rarity, tiers, buckets, labels, or probability classes.
- Do not change pricing pipeline order, drift behavior, active event lifecycle, scheduler selection, blocking semantics, quote settlement, or public snapshot disclosure.
- Do not mix pressure-ladder market core migration cleanup into this card.
- Treat this as unsafe for already-migrated shared/prod databases unless a separate operational reset plan exists.

## Validation Commands

Run from `java/`:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketEventMigrationTest --tests '*MarketEvent*Test' --tests '*DashboardMarketEventTemplateApiIntegrationTest' --tests '*DashboardMarketEventAdminApiIntegrationTest' --tests '*MarketContractIntegrationTest'
```

Then run:

```bash
rtk ./gradlew test
```

## Out of Scope

- Production Flyway repair, data export/import, or deployed-database migration planning.
- Changing market event scheduling probabilities or template defaults.
- Dashboard frontend changes.
- Pressure-ladder market item, quote, category, or segment cleanup.

## Suggested Commit Message

`refactor(market-events): normalize event migration history`

## Completion Notes

- Updated `V22__create_market_events_tables.sql` to create event template and instance tables without obsolete event classification columns.
- Removed `V25__drop_market_event_rarity.sql` because fresh schemas no longer create the obsolete columns.
- Kept Flyway version numbers as historical identifiers and deleted the obsolete follow-up version rather than renumbering later migrations.
- Updated event migration tests to assert the current event schema at `V22` and across the full migration chain.
- Verified the strict runtime/test scan for the removed event classification terminology has no hits.
- Validated with focused migration tests, broader market/card tests, and the full Gradle test suite.
