# Market Pressure Ladder — Sigmoid Pricing Proposal

## Purpose

This document describes a proposed replacement for the current bounded segment-capacity market aggregate.

The goal is to keep the existing API contract shape (`snapshot -> quote -> execute`) while changing the internal pricing model from finite positive segments into an unbounded pressure ladder centered on segment `0`.

`craftalism-api` owns this behavior because pricing, quotes, execution, stock semantics, stale detection, and durable market state are authoritative backend responsibilities.

`craftalism-market` must continue to consume snapshots, request quotes, and execute quote-backed trades without reimplementing price logic locally.

---

## Current Model Summary

The current aggregate uses finite segments:

- segment indexes start at `0`
- each segment has `maxCapacity`
- each segment has `remainingCapacity`
- buys consume remaining capacity from low indexes upward
- sells restore capacity from high consumed indexes downward
- maximum stock is bounded by `sum(maxCapacity)`

This works for bounded supply, but it makes oversupply impossible once all capacity is restored.

---

## Proposed Model

Replace finite capacity layers with a pressure ladder:

```text
... -3, -2, -1, 0, 1, 2, 3 ...
```

Segment meaning:

- `0` is the base/equilibrium segment
- negative segments represent oversupply and depreciated price
- positive segments represent demand pressure and overvalued price

Stock means the number of items represented by one segment.

Example:

```text
segmentSize = 50

segment -2 covers 50 items of oversupply pressure
segment -1 covers 50 items of oversupply pressure
segment  0 is equilibrium
segment  1 covers 50 items of demand pressure
segment  2 covers 50 items of demand pressure
```

The market should not persist infinite segment rows. It should persist compact aggregate state and derive segment traversal deterministically.

---

## Authoritative State

The proposed aggregate root remains `MarketItem`, but the authoritative mutable pricing state changes.

Suggested fields:

- `itemId`
- `baseUnitPrice`
- `minUnitPrice`
- `maxUnitPrice`
- `segmentSize`
- `priceSensitivity`
- `netPosition`
- `lastUpdatedAt`
- `blocked`
- `operating`

Field meaning:

- `baseUnitPrice`: price at segment `0`
- `minUnitPrice`: lower saturation bound
- `maxUnitPrice`: upper saturation bound
- `segmentSize`: number of items per segment
- `priceSensitivity`: curve steepness
- `netPosition`: signed pressure position
- `lastUpdatedAt`: regeneration timestamp

`netPosition` is the key execution state:

```text
netPosition = 0      equilibrium
netPosition > 0      demand pressure / overvalued
netPosition < 0      oversupply pressure / depreciated
```

Buy and sell direction:

```text
BUY  increases netPosition
SELL decreases netPosition
```

---

## Derived Projections

Stored projections may remain for snapshot convenience, but they must be derived from authoritative state.

Suggested projections:

- `currentStock`
- `marketMomentum`
- `buyUnitEstimate`
- `sellUnitEstimate`
- `variationPercent`

Recommended meanings:

- `currentStock`: derived display value representing market pressure or available market stock, not a hard bounded inventory cap
- `marketMomentum`: current signed segment index
- `buyUnitEstimate`: price for the next buy unit
- `sellUnitEstimate`: price for the next sell unit
- `variationPercent`: price movement from `baseUnitPrice`

The implementation must clearly document the public meaning of `currentStock`, because under this model it is not the same as the old bounded stock total.

---

## Segment Derivation

Segment index is derived from `netPosition` and `segmentSize`.

Use floor division so negative positions map correctly:

```text
segmentIndex = floorDiv(netPosition, segmentSize)
offsetInSegment = floorMod(netPosition, segmentSize)
```

Examples with `segmentSize = 50`:

```text
netPosition = 0      segment 0
netPosition = 49     segment 0
netPosition = 50     segment 1
netPosition = -1     segment -1
netPosition = -50    segment -1
netPosition = -51    segment -2
```

This mapping makes segment `0` cover `[0, segmentSize - 1]`, while negative pressure immediately enters depreciated segment `-1`.

That behavior should be confirmed before implementation.

---

## Price Curve

Use a bounded sigmoid-like saturation curve anchored at the base price.

Avoid a raw logistic sigmoid for the first implementation because raw logistic math naturally places segment `0` at the midpoint between `minUnitPrice` and `maxUnitPrice`, which may not equal the desired base price.

Recommended anchored saturation:

```text
pressure(n) = 1 - exp(-priceSensitivity * n)
```

For positive segments:

```text
price = baseUnitPrice + (maxUnitPrice - baseUnitPrice) * pressure(segmentIndex)
```

For negative segments:

```text
price = baseUnitPrice - (baseUnitPrice - minUnitPrice) * pressure(abs(segmentIndex))
```

For segment `0`:

```text
price = baseUnitPrice
```

After calculation:

```text
unitPrice = clamp(round(price), minUnitPrice, maxUnitPrice)
```

Properties:

- segment `0` always equals `baseUnitPrice`
- extreme buying approaches `maxUnitPrice`
- extreme selling approaches `minUnitPrice`
- price never reaches zero or negative unless explicitly configured incorrectly
- volatility is bounded

---

## Quote Planning

Quotes remain quantity-sensitive.

The planner must walk through virtual segments instead of persisted segment rows.

For a buy:

1. Start at current `netPosition`.
2. Traverse upward for `quantity` units.
3. Split the quantity by segment boundaries.
4. Price each slice using the segment price.
5. Sum the total.
6. Return effective unit price as `ceil(totalPrice / quantity)`.

For a sell:

1. Start at current `netPosition`.
2. Traverse downward for `quantity` units.
3. Split the quantity by segment boundaries.
4. Price each slice using the segment price.
5. Sum the total.
6. Return effective unit price as `ceil(totalPrice / quantity)`.

The quote must include:

- item id
- side
- quantity
- total price
- effective unit price
- snapshot version
- quote token
- expiry

Clients still must not compute totals locally.

---

## Execution

Execution keeps the existing quote-backed flow:

1. Resolve player.
2. Load quote.
3. Reject expired or consumed quote.
4. Verify player, item, side, quantity, and snapshot version.
5. Verify current snapshot still matches the quote snapshot.
6. Consume the quote token.
7. Lock the item aggregate.
8. Rebuild the trade plan from authoritative state.
9. Verify the rebuilt plan matches quoted total and unit price.
10. Apply balance settlement.
11. Mutate `netPosition`.
12. Recompute projections.
13. Persist.

Mutation:

```text
BUY  netPosition += quantity
SELL netPosition -= quantity
```

The execute operation must remain single-use per `quoteToken`.

---

## Regeneration

Regeneration should move `netPosition` toward `0`.

Suggested deterministic rule:

```text
regenQuantity = ticks * baseRegenQuantity
```

Apply direction:

```text
if netPosition > 0:
    netPosition = max(0, netPosition - regenQuantity)

if netPosition < 0:
    netPosition = min(0, netPosition + regenQuantity)
```

This means:

- overvalued demand pressure cools down over time
- oversupply pressure recovers over time
- fully balanced items remain at `0`

Optional future tuning:

- stronger regeneration when pressure magnitude is high
- item-specific regeneration speed
- category-specific regeneration speed

---

## Snapshot Contract Impact

The public endpoint shape can remain unchanged:

```text
GET /api/market/snapshot
POST /api/market/quotes
POST /api/market/execute
```

No client should need to understand the sigmoid curve.

Potential semantic change:

- `currentStock` currently means bounded available stock.
- Under this model, it may need to mean market pressure, available liquidity, or signed/absolute stock pressure.

Recommended contract choice:

Keep `currentStock` as a non-negative display value only if the UI still needs a stock-like number.

Add a new field only if the client must distinguish supply pressure from demand pressure:

```text
marketPressure: -125
marketSegment: -3
```

If adding fields, keep them informational and do not let clients compute prices from them.

---

## Rejection Semantics

This model removes bounded stock exhaustion unless explicit limits are added.

Existing rejection codes that still apply:

- `STALE_QUOTE`
- `ITEM_BLOCKED`
- `ITEM_NOT_OPERATING`
- `INSUFFICIENT_FUNDS`
- `MARKET_CLOSED`
- `INVALID_QUANTITY`
- `RATE_LIMITED`
- `QUOTE_EXPIRED`
- `API_UNAVAILABLE`
- `UNKNOWN_ITEM`

`INSUFFICIENT_STOCK` needs a decision:

- remove it from normal market pressure trading, or
- reserve it for configured hard position limits, or
- keep it only for legacy/limited items

Recommended:

Keep `INSUFFICIENT_STOCK` available, but only emit it when an item has configured hard trade bounds.

---

## Configuration

Each item should define:

- `baseUnitPrice`
- `minUnitPrice`
- `maxUnitPrice`
- `segmentSize`
- `priceSensitivity`
- `baseRegenQuantity`
- optional `minNetPosition`
- optional `maxNetPosition`

Example:

```text
itemId = wheat
baseUnitPrice = 50000
minUnitPrice = 25000
maxUnitPrice = 150000
segmentSize = 50
priceSensitivity = 0.08
baseRegenQuantity = 5
```

Hard validation:

- `baseUnitPrice > 0`
- `minUnitPrice > 0`
- `maxUnitPrice >= baseUnitPrice`
- `minUnitPrice <= baseUnitPrice`
- `segmentSize > 0`
- `priceSensitivity > 0`
- `baseRegenQuantity >= 0`

---

## Persistence Migration

This is not a small migration of the existing segment table.

The current model stores:

- `market_items`
- `market_segments`
- `market_quotes`

The new model likely needs:

- market item pricing configuration
- signed `netPosition`
- no persisted segment rows for normal operation

Migration options:

1. Add new columns to `market_items` and stop using `market_segments`.
2. Create a new aggregate table, migrate item metadata, then drop/deprecate `market_segments`.
3. Keep `market_segments` only for audit/history during a compatibility period.

Recommended first implementation path:

1. Add new columns while keeping old columns temporarily.
2. Implement a new planner behind the same service contract.
3. Backfill `netPosition` from legacy state.
4. Run contract tests against quote and execute behavior.
5. Remove or archive old segment persistence only after confidence.

Legacy backfill suggestion:

```text
legacy pressure = totalCapacity - currentStock
netPosition = legacy pressure
```

This maps consumed stock to positive demand pressure. It does not preserve every detail of prior segment state, but the current finite model is not semantically equivalent to the new pressure ladder.

Backfill must be reviewed item by item before production use.

---

## Invariants

Aggregate-level:

- `segmentSize > 0`
- `minUnitPrice > 0`
- `minUnitPrice <= baseUnitPrice <= maxUnitPrice`
- `priceSensitivity > 0`
- `netPosition` must not overflow when applying quantity
- projected segment price must always be within `[minUnitPrice, maxUnitPrice]`

Execution-level:

- quoted total must equal deterministic traversal total
- executed quantity must equal requested quantity
- quote tokens remain single-use
- failed settlement must not mutate market pressure
- successful buy increases `netPosition`
- successful sell decreases `netPosition`
- regeneration only moves `netPosition` toward `0`

Snapshot-level:

- `snapshotVersion` changes when authoritative state or trade-affecting config changes
- clients treat `snapshotVersion` as opaque
- clients treat quote tokens as opaque

---

## Testing Strategy

Unit tests:

- segment derivation for positive positions
- segment derivation for negative positions
- boundary behavior at exact segment multiples
- anchored price equals base at segment `0`
- positive prices approach max
- negative prices approach min
- clamping and rounding
- quote traversal across one segment
- quote traversal across multiple positive segments
- quote traversal across multiple negative segments
- sell crossing from positive through zero into negative
- buy crossing from negative through zero into positive

Integration tests:

- snapshot includes stable opaque version
- quote rejects stale snapshot
- execute rejects stale quote
- execute consumes quote once
- buy mutates player balance and increases pressure
- sell mutates player balance and decreases pressure
- insufficient funds leaves market pressure unchanged
- regeneration moves positive pressure toward zero
- regeneration moves negative pressure toward zero
- blocked and non-operating items reject quote/execute

Migration tests:

- default catalog creates valid pricing config
- legacy market state maps to deterministic `netPosition`
- snapshot contract remains compatible for existing clients

---

## Open Questions For Grill-Me

Before implementation, confirm these decisions:

1. Should `currentStock` remain in the snapshot, and what should it mean under signed pressure?
2. Should players be allowed to sell infinitely, bounded only by price approaching `minUnitPrice`?
3. Should each item have optional `minNetPosition` and `maxNetPosition` hard bounds?
4. Should sell quotes pay the same segment price as buy quotes when traversing the same segment?
5. Should segment `0` cover `[0, segmentSize - 1]`, or should equilibrium be centered around zero with half a segment on each side?
6. Should regeneration be constant, pressure-weighted, or item-specific?
7. Should `variationPercent` be derived from current buy estimate, sell estimate, or midpoint price?
8. How aggressive should `priceSensitivity` be for common resources versus rare resources?
9. Should admin/config changes invalidate all active quotes immediately?
10. Should the old finite segment model remain available for specific limited-supply items?

---

## Implementation Order

Recommended sequence:

1. Finalize the semantics in this document.
2. Update the aggregate source-of-truth documentation.
3. Add pricing configuration and `netPosition` persistence.
4. Implement a pure pressure-ladder planner with unit tests.
5. Swap quote planning to the new planner.
6. Swap execute mutation to `netPosition`.
7. Implement regeneration toward zero.
8. Update snapshot projection and version hashing.
9. Add migration/backfill.
10. Re-run market contract integration tests.
11. Update client-facing contract docs only if response semantics or fields change.

