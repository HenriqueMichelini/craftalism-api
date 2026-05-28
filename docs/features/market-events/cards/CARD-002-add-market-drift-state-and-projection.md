# CARD-002: Add Market Drift State And Projection

## Status

completed

## Objective

Add unnamed per-item drift as a small bounded modifier in market price projection.

## Context

Drift is not a named event. It is ambient market movement that keeps prices alive while remaining separate from player-driven pressure and named event modifiers.

This card depends on the shared pricing pipeline from `CARD-011` so drift cannot be applied to snapshots without also affecting quote planning and quote-backed execution.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`
- `CARD-009-split-snapshot-freshness-from-quote-execution-validity.md`
- `CARD-010-persist-quote-pricing-context.md`
- `CARD-011-introduce-market-pricing-pipeline.md`

## Expected Behavior

Each tradable market item has drift state separate from pressure regeneration state. Snapshot estimates, quote planning, execute pricing, and `variationPercent` include drift through the shared pricing pipeline after pressure price derivation and before named event modifiers and final min/max clamp. Drift remains tightly capped, mean-reverting or otherwise bounded, and does not change `netPosition`.

## Acceptance Criteria

- [ ] Persist per-item drift state and drift timestamp separately from pressure `lastUpdatedAt`.
- [ ] Apply drift through the shared pricing pipeline before named event modifiers and final clamp.
- [ ] Evaluate drift on the lazy or timed market update rhythm using the MVP cadence and bounded movement rules from the contract.
- [ ] Drift evaluation works for balanced items where `netPosition == 0`.
- [ ] Drift updates do not alter pressure regeneration timing.
- [ ] SELL prices continue to derive from drift-adjusted buy prices using `sellPricePercentage`.
- [ ] `variationPercent` reflects the actual current buy estimate including pressure and drift.
- [ ] Drift does not mutate pressure, hard pressure bounds, quote lifecycle, or settlement behavior.
- [ ] Snapshot version changes when drift-affecting state changes.
- [ ] Quote pricing context records the drift value or revision used for the quote.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/
java/src/main/resources/db/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/
```

## Constraints

- Do not implement named events.
- Do not change pressure curve formulas.
- Do not change public DTO shape except values already exposed as prices/variation.
- Do not reuse pressure `lastUpdatedAt` for drift timing.
- Keep drift small and bounded by config or constants following the cadence, movement band, and cumulative constraint documented in the contract.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest
```

Run from `java/`.

## Out of Scope

- Event templates.
- Scheduler windows.
- Admin APIs.
- Client UI labels.

## Completion Notes

- Added durable per-item drift multiplier, revision, and evaluated-at timestamp separate from pressure `lastUpdatedAt`.
- Added bounded hourly drift evaluation on the market read/update path, including balanced items where `netPosition == 0`, without mutating pressure.
- Routed drift multipliers through the shared pricing pipeline for snapshots and quote planning; SELL prices continue to derive from drift-adjusted BUY prices.
- Included drift state in snapshot-version hashing and quote pricing context.
- Added V23 migration and targeted planner, projector, read-service, quote-service, and migration coverage.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketReadServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteServiceTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketDriftMigrationTest` passed from `java/`.
