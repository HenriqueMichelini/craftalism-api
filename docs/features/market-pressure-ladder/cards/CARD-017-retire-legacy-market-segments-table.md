# CARD-017: Retire Legacy Market Segments Table

## Status

completed

## Objective

Remove the legacy `market_segments` persistence surface after confirming pressure-ladder runtime behavior no longer depends on persisted segment rows.

## Context

RefactorFirst identified `MarketItem -> MarketSegment` as a relationship removal priority. The pressure-ladder contract makes `market_items` the authoritative market aggregate, and `CARD-010` removed normal runtime dependency on `market_segments`.

This card scopes a persistence cleanup only after runtime code and tests prove legacy segment rows are no longer needed for snapshot, quote, execute, catalog initialization, regeneration, or projection behavior.

## Required Reading

- `../contract.md`
- `CARD-010-remove-runtime-segment-dependency.md`

## Expected Behavior

Normal market behavior remains pressure-based and unchanged while the obsolete `market_segments` table, entity, ID type, repository, and runtime test assumptions are removed through a forward-only cleanup.

## Acceptance Criteria

- [ ] Confirm no normal runtime code depends on `MarketSegmentRepository`.
- [ ] Confirm no normal runtime code reads or writes `MarketItem.segments`.
- [ ] Add a forward Flyway migration that drops `market_segments`.
- [ ] Remove `MarketSegment`, `MarketSegmentId`, and `MarketSegmentRepository` if no migration or audit code still needs them.
- [ ] Remove runtime tests that assert empty segment collections on `MarketItem`.
- [ ] Preserve historical migrations without editing them.
- [ ] Snapshot, quote, execute, catalog bootstrap, pressure regeneration, and migration tests still pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketItem.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketSegment.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketSegmentId.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketSegmentRepository.java
java/src/main/resources/db/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/
```

## Constraints

- Do not edit historical migrations.
- Do not change pressure pricing, quote planning, execution, snapshot, rejection, authentication, authorization, or balance behavior.
- Do not change public API DTOs or endpoint contracts.
- Do not implement client behavior in this repository.
- Use a forward migration only.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`.

Fallback for focused iteration before the full suite:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketPressureStateMigrationTest
```

Run from `java/`.

## Out of Scope

- Rewriting old Flyway migrations.
- Reintroducing finite stock or segment-capacity behavior.
- Changing market pressure-ladder calculations.
- Changing public market contracts.
- Updating `craftalism-market` or any other consumer repository.

## Suggested Commit Message

`refactor(craftalism-api): retire legacy market segments table`

## Completion Notes

- Removed the `MarketItem.segments` JPA relationship and the legacy `MarketSegment`, `MarketSegmentId`, and `MarketSegmentRepository` persistence surface.
- Added forward migration `V16__drop_legacy_market_segments.sql` to drop `market_segments` without editing historical migrations.
- Removed runtime tests that asserted empty `MarketItem` segment collections and kept migration coverage for legacy backfill plus final table removal.
- Validation passed from `java/`: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketPressureStateMigrationTest`.
- Validation passed from `java/`: `rtk ./gradlew test`.
