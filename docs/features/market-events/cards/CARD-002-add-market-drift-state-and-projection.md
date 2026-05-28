# CARD-002: Add Market Drift State And Projection

## Status

planned

## Objective

Add unnamed per-item drift as a small bounded modifier in market price projection.

## Context

Drift is not a named event. It is ambient market movement that keeps prices alive while remaining separate from player-driven pressure and named event modifiers.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Each tradable market item has drift state. Snapshot estimates, quote planning, execute pricing, and `variationPercent` include drift after pressure price derivation and before final min/max clamp. Drift remains tightly capped and does not change `netPosition`.

## Acceptance Criteria

- [ ] Persist per-item drift state and timestamp needed to derive current drift.
- [ ] Apply drift multiplicatively to pressure-derived buy prices before final clamp.
- [ ] Evaluate drift on the lazy/timed market update rhythm using the MVP cadence and bounded movement rules from the contract.
- [ ] SELL prices continue to derive from drift-adjusted buy prices using `sellPricePercentage`.
- [ ] `variationPercent` reflects the actual current buy estimate including pressure and drift.
- [ ] Drift does not mutate pressure, hard pressure bounds, quote lifecycle, or settlement behavior.
- [ ] Snapshot version changes when drift-affecting state changes.

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
