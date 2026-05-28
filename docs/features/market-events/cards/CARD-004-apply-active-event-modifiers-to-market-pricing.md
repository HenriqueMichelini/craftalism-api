# CARD-004: Apply Active Event Modifiers To Market Pricing

## Status

planned

## Objective

Apply the active named market event modifier to snapshot, quote, and execute pricing.

## Context

Named events are explicit temporary modifiers. They apply to current pressure-plus-drift price before final min/max clamp. Prices return immediately to pressure plus drift when the event ends.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

At most one active named event can affect a market price in MVP. Active price events modify eligible item prices multiplicatively, then clamp to item min/max. Quotes preserve event conditions until quote expiry unless the item becomes blocked before execution.

## Acceptance Criteria

- [ ] Snapshot buy/sell estimates include active event modifiers for affected items.
- [ ] Quote totals preserve the event modifier active when the quote was created.
- [ ] Execute uses the quote-preserved event conditions for price settlement.
- [ ] If an event ends before quote execution, the quote price remains valid until expiry.
- [ ] If an item becomes blocked before execution, execution rejects.
- [ ] `variationPercent` reflects actual pressure, drift, and active event pricing.
- [ ] Ended events stop affecting new snapshots and new quotes immediately.

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
