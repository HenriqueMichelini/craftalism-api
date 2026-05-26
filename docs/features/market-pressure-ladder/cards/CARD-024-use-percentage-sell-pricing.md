# CARD-024: Use Percentage Sell Pricing

## Status

completed

## Objective

Make sell estimates and SELL quote pricing use a per-item sell price percentage derived from the current pressure buy price.

## Context

CARD-022 enforced only a one-unit display spread between buy and sell estimates. That keeps values different, but after large trades the displayed sell price can remain nearly equal to the displayed buy price. The required behavior is a stable item-specific percentage spread: `sell_price = buy_price * sell_percentage`.

This card changes backend-owned market pricing semantics. It explicitly scopes a persistence and dashboard API config addition because each item must define or use a sell percentage.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Each market item has a `sellPricePercentage` value, defaulting to `0.7000`. Display sell estimates are calculated from the current display buy estimate using that percentage. SELL quote and execute pricing use the same percentage against pressure-derived buy prices for the traversed positions, so seller payouts keep a stable percentage spread while remaining pressure-sensitive and quantity-sensitive.

BUY quote planning remains pressure-position based and unchanged. Pressure mutation direction, hard pressure bounds, stale quote behavior, quote lifecycle, settlement behavior, regeneration, and snapshot version semantics remain unchanged except that `sellPricePercentage` is trade-affecting config and must participate in snapshot version hashing.

## Acceptance Criteria

- [x] `market_items` has a non-null `sell_price_percentage` column with a default of `0.7000`.
- [x] Market item entity, default catalog seeds, admin create/update requests, and dashboard responses expose `sellPricePercentage`.
- [x] Validation rejects sell percentages that are not greater than `0` and less than `1`.
- [x] Derived projections set `sellUnitEstimate` from `buyUnitEstimate * sellPricePercentage`, not from `netPosition - 1` raw pressure price.
- [x] SELL quote totals and execute payouts use percentage sell pricing while preserving pressure traversal and quantity sensitivity.
- [x] BUY quote totals remain unchanged.
- [x] `snapshotVersion` changes when `sellPricePercentage` changes.
- [x] Docs describe the percentage sell pricing rule and no longer describe one-unit spread adjustment as the intended spread model.
- [x] Focused service, controller, catalog, and migration tests cover the new field and pricing behavior.

## Expected Files to Change

```text
docs/features/market-pressure-ladder/contract.md
docs/market-pressure-ladder-sigmoid-pricing.md
docs/market-contract-mvp.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketItem.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketItemCreateRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketItemUpdateRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketItemResponseDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/mapper/MarketItemMapper.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DashboardMarketItemService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItem.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItemBuilder.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DefaultMarketCatalog.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializer.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java
java/src/main/resources/db/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/
docs/features/market-pressure-ladder/cards/CARD-024-use-percentage-sell-pricing.md
```

## Constraints

- Do not change BUY pressure pricing.
- Do not change pressure mutation direction, hard pressure-bound rejection semantics, quote lifecycle, stale quote behavior, settlement behavior, or regeneration behavior.
- Do not change snapshot, quote, or execute DTO field names except adding dashboard market item config field `sellPricePercentage`.
- Do not expose `sellPricePercentage` in public market snapshot item DTOs unless required by implementation.
- Do not implement client behavior in this repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketSellPricePercentageMigrationTest
```

Run from `java/`.

If focused validation passes, run:

```bash
rtk ./gradlew clean build
```

Run from `java/`.

## Out of Scope

- Client-side formatting or display changes in `craftalism-market`.
- Changing the pressure curve used for BUY prices.
- Changing hard pressure-bound semantics.
- Repricing already persisted trade history rows.
- Supporting per-player or per-transaction sell percentages.

## Completion Notes

- Added `sell_price_percentage NUMERIC(5, 4) NOT NULL DEFAULT 0.7000` with a database check requiring `0 < sell_price_percentage < 1`.
- Added `sellPricePercentage` to `MarketItem`, default catalog seeds, dashboard create/update requests, dashboard responses, mapper output, and validation.
- Changed derived projections so `sellUnitEstimate` is calculated from `buyUnitEstimate * sellPricePercentage`.
- Changed SELL quote planning to traverse downward from current `netPosition` and apply `sellPricePercentage` to each traversed pressure-derived buy price. BUY quote pricing is unchanged.
- Included `sellPricePercentage` in snapshot-version hashing because it affects SELL quote totals and execute payouts.
- Updated pressure-ladder docs and market contract docs to describe percentage sell pricing.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketSellPricePercentageMigrationTest` from `java/` passed.
- Validation: `rtk ./gradlew clean build` from `java/` passed after approved access to the Gradle wrapper cache under `~/.gradle`.
