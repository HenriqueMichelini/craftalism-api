# CARD-011: Introduce Market Pricing Pipeline

## Status

completed

## Objective

Create one shared market pricing pipeline used by snapshot projection, quote planning, and quote-backed execution.

## Context

Current market pricing derives pressure prices directly in the trade planner. Drift and named event modifiers must not be added in only one path, because snapshots, quotes, and execution must remain consistent.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

The backend has a single pricing path for pressure-derived buy prices, optional drift adjustment, optional named event adjustment, final min/max clamp, and sell price derivation from the adjusted buy price using `sellPricePercentage`. Initial implementation may have neutral drift and no named event modifier, but the extension points must be explicit and tested.

## Acceptance Criteria

- [ ] Snapshot projection, quote planning, and execute verification/settlement use the same pricing pipeline.
- [ ] The pipeline preserves pressure-ladder segment traversal and min/max clamps.
- [ ] The pipeline derives SELL prices from the adjusted BUY price using `sellPricePercentage`.
- [ ] The neutral pipeline produces the same prices as the current pressure-only behavior.
- [ ] Tests cover neutral buy/sell estimates, multi-segment quote totals, sell percentage behavior, and variation percent.
- [ ] The pipeline has explicit inputs for drift and named event context without requiring those features to be implemented in this card.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not add drift persistence.
- Do not add named event persistence.
- Do not change public DTO shape.
- Do not change pressure mutation, quote lifecycle, or settlement behavior.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest
```

Run from `java/`.

## Out of Scope

- Drift updates.
- Named event effects.
- Event blocking.
- Scheduler or admin APIs.

## Completion Notes

- Added `MarketPricingPipeline` as the shared internal path for pressure-derived buy price, neutral drift input, neutral named-event input, final clamp, and sell percentage derivation.
- Routed market snapshot projection, quote planning, and quote-backed execution planning through the neutral pipeline via `MarketTradePlanner`.
- Preserved existing pressure-ladder traversal, min/max clamps, sell percentage behavior, variation calculation, quote lifecycle, pressure mutation, and public DTO shape.
- Added planner coverage proving the neutral pipeline matches current pressure-only buy/sell prices.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest` passed from `java/`.
