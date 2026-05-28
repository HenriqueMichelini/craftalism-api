# CARD-004: Apply Active Event Modifiers To Market Pricing

## Status

completed

## Objective

Apply the active named market event modifier to snapshot, quote, and execute pricing.

## Context

Named events are explicit temporary modifiers. They apply to current pressure-plus-drift price before final min/max clamp. Prices return immediately to pressure plus drift when the event ends.

This card is unsafe until quote execution validity, quote pricing context, the shared pricing pipeline, and active event lifecycle/locking are implemented.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`
- `CARD-009-split-snapshot-freshness-from-quote-execution-validity.md`
- `CARD-010-persist-quote-pricing-context.md`
- `CARD-011-introduce-market-pricing-pipeline.md`
- `CARD-012-add-active-event-lifecycle-and-locking.md`

## Expected Behavior

At most one active named event can affect a market price in MVP. Active price events modify eligible item prices through the shared pricing pipeline, after pressure-plus-drift pricing and before final min/max clamp. Quotes preserve the event pricing context active when the quote was created until quote expiry unless the item becomes effectively blocked before execution.

## Acceptance Criteria

- [ ] Snapshot buy/sell estimates include active event modifiers for affected items.
- [ ] Quote totals preserve the event modifier active when the quote was created.
- [ ] Execute settles using the stored quote price promise and quote pricing context, not a recomputed current event price.
- [ ] If an event ends before quote execution, the quote price remains valid until expiry.
- [ ] If an item becomes effectively blocked before execution, execution rejects.
- [ ] `variationPercent` reflects actual pressure, drift, and active event pricing.
- [ ] Ended events stop affecting new snapshots and new quotes immediately.
- [ ] Event modifiers are included in snapshot version generation for new snapshots and quote creation.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
```

## Constraints

- Do not change pressure mutation direction.
- Do not change `sellPricePercentage` semantics.
- Do not allow event modifiers to bypass min/max bounds.
- Do not expose exact multipliers in public market responses.
- Do not reintroduce current-snapshot equality as a standalone execute-time quote rejection.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

## Out of Scope

- Scheduler.
- Admin APIs.
- Public event history.
- Multiple simultaneous named events.

## Completion Notes

- Added active named-event pricing resolution for item, item-set, category, and market-wide scopes.
- Routed active event multipliers through the shared pricing pipeline after drift and before final clamps for snapshots and quote planning.
- Stored quote-time named event instance/effect metadata in quote pricing context.
- Changed execute settlement to use the stored quote unit and total price after validity, availability, and pressure-bound checks, so quotes survive event/drift price changes until expiry.
- Included active event pricing metadata and resulting projections in snapshot-version generation without exposing exact multipliers publicly.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteServiceTest` passed from `java/`.
