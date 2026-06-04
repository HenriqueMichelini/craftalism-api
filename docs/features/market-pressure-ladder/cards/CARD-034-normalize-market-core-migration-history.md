# CARD-034: Normalize Market Core Migration History

## Status

completed

## Objective

Restructure the market core Flyway migrations so the clean migration chain matches the current pressure-ladder domain model without obsolete segment-table, carrot-specific, duplicate quote-status, or incremental category/sell-percentage artifacts.

## Context

Migration audit confirmed that the current chain preserves historical implementation steps that no longer make sense as a current-domain schema:

- `V9__create_market_quotes_table.sql` already creates `status`, `resolved_at`, and `idx_market_quotes_status`, then `V10__add_market_quote_status_columns.sql` adds the same surface again with `IF NOT EXISTS`.
- `V12__create_market_segments_table.sql`, `V13__backfill_market_segments_from_legacy_state.sql`, `V14__lower_carrot_restored_segment_prices.sql`, `V15__add_market_pressure_state.sql`, and `V16__drop_legacy_market_segments.sql` encode the retired finite segment model only to convert it into pressure state and immediately drop it.
- `V14__lower_carrot_restored_segment_prices.sql` is a carrot-specific data patch that is inconsistent with the current pressure-ladder catalog/defaults model.
- `V18__add_market_sell_price_percentage.sql` adds a current required market item config after the fact.
- `V19__create_market_categories_table.sql` creates categories and `V20__add_market_category_icon_key.sql` immediately extends the same table.

This card intentionally bypasses the usual historical Flyway preservation rule for a local/current-schema cleanup. Do not run this card against any database that has already applied the existing migration versions unless an explicit migration reset/repair plan is approved.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`
- `../../dashboard-crud-api/contract.md`

## Expected Behavior

Fresh migrations create only the schema required by the current backend code and pressure-ladder domain. Normal runtime remains pressure-based, category-backed, sell-percentage-aware, and independent of `market_segments`.

The final schema after applying all migrations must remain compatible with the current JPA models, repositories, catalog bootstrapper, dashboard market item CRUD behavior, quote lifecycle, trade history, drift/event follow-up migrations, and market API contract.

## Acceptance Criteria

- [ ] `market_segments` is not created, populated, patched, read, or dropped by the clean migration chain.
- [ ] The carrot-specific restored segment price migration is removed.
- [ ] Quote status/resolution fields and indexes are defined once in the quote table creation path.
- [ ] `market_items` current pressure-ladder columns are created directly in the clean market item schema or in one coherent pressure-state migration that does not depend on `market_segments`.
- [ ] `sell_price_percentage` is present with `0.7000` default and the existing `0 < value < 1` check in the clean market item path.
- [ ] `market_categories` is created with `icon_key` directly instead of requiring a same-table follow-up.
- [ ] Migration tests no longer assert or depend on intermediate `market_segments` or carrot patch behavior.
- [ ] Final migrated schema validates against the current JPA mappings under the docker/validate profile.
- [ ] Public API behavior, quote lifecycle, pressure pricing, sell pricing, category responses, trade history, drift, and event behavior are unchanged.
- [ ] The card documents any Flyway version renumbering or deletion policy used during implementation.

## Expected Files to Change

```text
java/src/main/resources/db/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
docs/features/market-pressure-ladder/cards/CARD-034-normalize-market-core-migration-history.md
```

## Constraints

- This is a migration-history cleanup only; do not change runtime pricing, quote planning, execute settlement, regeneration, snapshot hashing, dashboard CRUD semantics, or DTO field names.
- Do not remove current required columns such as `market_momentum`, derived projections, pressure config, sell percentage, category relationship, or drift/event columns required by later migrations.
- Do not introduce new client-visible behavior.
- Do not mix market event table cleanup into this card except where later migrations must still apply cleanly after the market core rewrite.
- Treat this as unsafe for already-migrated shared/prod databases unless a separate operational reset plan exists.

## Validation Commands

Run from `java/`:

```bash
rtk ./gradlew test --tests 'io.github.HenriqueMichelini.craftalism.api.migration.*'
rtk ./gradlew test --tests '*MarketCatalogBootstrapperTest' --tests '*MarketTradePlannerTest' --tests '*MarketQuoteServiceTest' --tests '*MarketTradeExecutorTest' --tests '*MarketSnapshotProjectorTest' --tests '*DashboardMarketItemCrudApiIntegrationTest' --tests '*MarketContractIntegrationTest'
```

Then run:

```bash
rtk ./gradlew test
```

If schema validation can be run locally against PostgreSQL, also run the docker/validate profile startup check.

## Out of Scope

- Production Flyway repair, data export/import, or deployed-database migration planning.
- Changing pressure-ladder formulas or catalog prices.
- Changing market event template/instance table history.
- Removing dashboard or public API fields.
- Backfilling historical trade history.

## Suggested Commit Message

`refactor(market): normalize core migration history`

## Completion Notes

- Folded current market item pressure-ladder columns, `market_momentum`, and `sell_price_percentage` into `V8__create_market_items_table.sql`.
- Removed the obsolete clean-history migrations for duplicate quote status, legacy market segments, carrot-specific segment prices, pressure backfill from segments, segment drop, sell percentage follow-up, and category icon follow-up.
- Kept Flyway version numbers as historical identifiers and deleted obsolete versions rather than renumbering later migrations.
- Updated `V19__create_market_categories_table.sql` to create `icon_key` directly.
- Updated migration tests to validate the current clean schema without constructing intermediate `market_segments` state.
- Validated with focused migration tests, broader market/card tests, and the full Gradle test suite.
