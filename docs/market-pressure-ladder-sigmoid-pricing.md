# Market Pressure Ladder — Sigmoid Pricing Implementation Plan

## Purpose

This document is the implementation plan for replacing the bounded segment-capacity market aggregate with a pressure ladder centered on segment `0`.

`craftalism-api` owns this behavior because pricing, quotes, execution, stock semantics, stale detection, regeneration, and durable market state are authoritative backend responsibilities.

`craftalism-market` consumes snapshots, requests quotes, and executes quote-backed trades. It must not reimplement price logic locally.

This plan supersedes `docs/aggregate-dynamic-pricing.md`.

---

## Implementation Position

The current implementation uses finite persisted segments:

- segment indexes start at `0`
- each segment has `maxCapacity`
- each segment has `remainingCapacity`
- buys consume remaining capacity from low indexes upward
- sells restore capacity from high consumed indexes downward
- maximum stock is bounded by `sum(maxCapacity)`

The new implementation removes bounded stock as the normal market constraint. It uses signed market pressure as the authoritative mutable pricing state.

This is a breaking market contract change for clients that interpret `currentStock` as available inventory.

---

## Target Model

Replace finite capacity layers with a pressure ladder:

```text
... -3, -2, -1, 0, 1, 2, 3 ...
```

Segment meaning:

- `0` is the base/equilibrium segment
- negative segments represent oversupply and depreciated price
- positive segments represent demand pressure and overvalued price

`segmentSize` is the number of pressure units represented by one segment.

Example:

```text
segmentSize = 50

segment -2 covers 50 items of oversupply pressure
segment -1 covers 50 items of oversupply pressure
segment  0 is equilibrium
segment  1 covers 50 items of demand pressure
segment  2 covers 50 items of demand pressure
```

The market must not persist infinite segment rows. It persists compact aggregate state and derives virtual segment traversal deterministically.

---

## Authoritative State

The aggregate root remains `MarketItem`.

Required authoritative fields:

- `itemId`
- `categoryId`
- `categoryDisplayName`
- `displayName`
- `iconKey`
- `currency`
- `baseUnitPrice`
- `minUnitPrice`
- `maxUnitPrice`
- `segmentSize`
- `priceSensitivity`
- `baseRegenQuantity`
- `netPosition`
- `minNetPosition`
- `maxNetPosition`
- `lastUpdatedAt`
- `blocked`
- `operating`

Field meaning:

- `baseUnitPrice`: price at segment `0`
- `minUnitPrice`: lower saturation bound
- `maxUnitPrice`: upper saturation bound
- `segmentSize`: pressure units per segment
- `priceSensitivity`: curve steepness
- `baseRegenQuantity`: deterministic pressure recovery amount per regeneration tick
- `netPosition`: signed pressure position
- `minNetPosition`: optional hard lower pressure bound
- `maxNetPosition`: optional hard upper pressure bound
- `lastUpdatedAt`: regeneration timestamp

`netPosition` is the key execution state:

```text
netPosition = 0      equilibrium
netPosition > 0      demand pressure / overvalued
netPosition < 0      oversupply pressure / depreciated
```

Trade direction:

```text
BUY  increases netPosition
SELL decreases netPosition
```

---

## Public Snapshot Contract

The endpoint shape remains unchanged:

```text
GET /api/market/snapshot
POST /api/market/quotes
POST /api/market/execute
```

The item snapshot contract changes.

Replace `currentStock` with pressure fields:

```text
marketPressure: signed long
marketSegment: signed long
pressureMagnitude: non-negative long
```

Meanings:

- `marketPressure`: the signed `netPosition`
- `marketSegment`: `floorDiv(netPosition, segmentSize)`
- `pressureMagnitude`: `abs(netPosition)` for display, sorting, or filtering

Do not silently redefine `currentStock`.

Recommended transition:

1. Remove `currentStock` from the market snapshot DTO if all clients can migrate in the same release.
2. Otherwise keep `currentStock` only as deprecated compatibility output and document that it is not authoritative.
3. Clients must migrate to `marketPressure`, `marketSegment`, and `pressureMagnitude`.

Clients must still treat prices, quote totals, quote tokens, and snapshot versions as backend-owned opaque values. Clients must not compute trade totals locally.

---

## Derived Projections

Stored projections may remain for snapshot convenience, but they must be derived from authoritative state.

Required derived values:

- `marketPressure = netPosition`
- `marketSegment = floorDiv(netPosition, segmentSize)`
- `pressureMagnitude = abs(netPosition)`
- `buyUnitEstimate = price for the next buy unit`
- `sellUnitEstimate = price for the next sell unit`
- `variationPercent = price movement from baseUnitPrice`

`marketMomentum` may be removed or kept as an internal alias for `marketSegment` during migration. Public clients should use `marketSegment`.

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

This mapping is intentional:

- segment `0` covers `[0, segmentSize - 1]`
- negative pressure immediately enters depreciated segment `-1`

---

## Price Curve

Use a bounded anchored saturation curve.

Do not use a raw logistic sigmoid for the first implementation because raw logistic math naturally places segment `0` at the midpoint between `minUnitPrice` and `maxUnitPrice`, which may not equal `baseUnitPrice`.

Anchored pressure:

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

Required properties:

- segment `0` equals `baseUnitPrice`
- extreme buying approaches `maxUnitPrice`
- extreme selling approaches `minUnitPrice`
- unit price stays within `[minUnitPrice, maxUnitPrice]`
- unit price never reaches zero or negative because `minUnitPrice > 0`

---

## Quote Planning

Quotes remain quantity-sensitive.

The planner must walk virtual segments instead of persisted segment rows.

For a buy:

1. Start at current `netPosition`.
2. Validate `netPosition + quantity` does not overflow.
3. Validate the result does not exceed `maxNetPosition` when configured.
4. Traverse upward for `quantity` units.
5. Split quantity by segment boundaries.
6. Price each slice using the segment price.
7. Sum the total.
8. Return effective unit price as `ceil(totalPrice / quantity)`.

For a sell:

1. Start at current `netPosition`.
2. Validate `netPosition - quantity` does not overflow.
3. Validate the result does not go below `minNetPosition` when configured.
4. Traverse downward for `quantity` units.
5. Split quantity by segment boundaries.
6. Price each slice using the segment price.
7. Sum the total.
8. Return effective unit price as `ceil(totalPrice / quantity)`.

The quote must include:

- item id
- side
- quantity
- total price
- effective unit price
- snapshot version
- quote token
- expiry

---

## Execution

Execution keeps the quote-backed flow:

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

Failed settlement must not mutate market pressure.

---

## Regeneration

Regeneration moves `netPosition` toward `0`.

Deterministic rule:

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
- balanced items remain at `0`

`baseRegenQuantity` must be item configuration so market tuning can happen per item without code changes.

---

## Rejection Semantics

The pressure model removes normal bounded stock exhaustion.

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

`INSUFFICIENT_STOCK` remains available only for configured hard pressure bounds:

- buy would exceed `maxNetPosition`
- sell would go below `minNetPosition`

Do not emit `INSUFFICIENT_STOCK` for ordinary pressure-ladder trading.

---

## Configuration Defaults

Each item must define:

- `baseUnitPrice`
- `minUnitPrice`
- `maxUnitPrice`
- `segmentSize`
- `priceSensitivity`
- `baseRegenQuantity`
- optional `minNetPosition`
- optional `maxNetPosition`

Recommended initial defaults for existing catalog items:

```text
baseUnitPrice = existing first/base segment price
minUnitPrice = max(1, round(baseUnitPrice * 0.50))
maxUnitPrice = round(baseUnitPrice * 3.00)
segmentSize = 50
priceSensitivity = 0.08
baseRegenQuantity = 1
minNetPosition = null
maxNetPosition = null
```

Use explicit per-item overrides where economy design requires them.

Hard validation:

- `baseUnitPrice > 0`
- `minUnitPrice > 0`
- `minUnitPrice <= baseUnitPrice`
- `maxUnitPrice >= baseUnitPrice`
- `segmentSize > 0`
- `priceSensitivity > 0`
- `baseRegenQuantity >= 0`
- `minNetPosition == null || minNetPosition <= 0`
- `maxNetPosition == null || maxNetPosition >= 0`
- when both bounds exist, `minNetPosition <= maxNetPosition`

---

## Snapshot Version

`snapshotVersion` must change when authoritative trade-affecting state or config changes.

Hash inputs must include:

- `itemId`
- `baseUnitPrice`
- `minUnitPrice`
- `maxUnitPrice`
- `segmentSize`
- `priceSensitivity`
- `baseRegenQuantity`
- `netPosition`
- `minNetPosition`
- `maxNetPosition`
- `blocked`
- `operating`
- `lastUpdatedAt` or another deterministic regeneration boundary value

Do not hash persisted derived projections as independent state.

Do not hash virtual segment lists.

---

## Persistence Migration

This is a replacement of the current segment model, not a small segment-table migration.

Current model stores:

- `market_items`
- `market_segments`
- `market_quotes`

Target model stores:

- market item metadata
- pressure-pricing configuration
- signed `netPosition`
- quote records

Normal operation must not require persisted `market_segments`.

Recommended implementation path:

1. Add new pressure-pricing columns to `market_items`.
2. Keep old segment columns/table temporarily only for migration and audit.
3. Backfill pressure config from existing catalog/segments.
4. Backfill `netPosition`.
5. Switch planner, projector, regeneration, and execution to pressure state.
6. Update market snapshot DTO and contract tests for pressure fields.
7. Remove or archive old segment persistence after the pressure path is verified.

Legacy backfill:

```text
totalCapacity = sum(existing market_segments.maxCapacity)
netPosition = totalCapacity - currentStock
```

This maps consumed finite stock to positive demand pressure:

- fully restored item: `netPosition = 0`
- partially consumed item: `netPosition > 0`
- old model cannot produce oversupply, so migration will not create negative `netPosition`

Backfill must be deterministic and auditable. If existing segment state violates invariants, fail migration or mark the item for manual repair; do not silently repair inconsistent state.

---

## Client Migration

Client implementations must update snapshot handling:

- stop treating `currentStock` as market inventory
- render `marketPressure` when signed pressure matters
- render `pressureMagnitude` when a non-negative display value is needed
- use `marketSegment` for pressure tier indicators
- keep using backend quote totals
- keep treating `snapshotVersion` and `quoteToken` as opaque

Client copy should avoid inventory language unless a hard bound is configured.

Recommended display semantics:

- `marketPressure > 0`: demand pressure
- `marketPressure == 0`: balanced
- `marketPressure < 0`: oversupply pressure

---

## Implementation Work Items

1. Update docs and contract references.
2. Add pressure-pricing fields and migration scripts.
3. Update `MarketItem` and remove normal-operation dependency on `MarketSegment`.
4. Replace `MarketTradePlanner` with virtual pressure traversal.
5. Update quote and execute bound checks.
6. Update regeneration to move `netPosition` toward `0`.
7. Update snapshot DTOs and snapshot version hashing.
8. Update default catalog seeding and validation.
9. Add migration/backfill tests.
10. Add unit tests for derivation, pricing, traversal, bounds, and regeneration.
11. Add integration tests for snapshot, quote, execute, stale quote, single-use quote, and insufficient funds.
12. Update client-facing contract docs.

---

## Invariants

Aggregate-level:

- `segmentSize > 0`
- `minUnitPrice > 0`
- `minUnitPrice <= baseUnitPrice <= maxUnitPrice`
- `priceSensitivity > 0`
- `baseRegenQuantity >= 0`
- `netPosition` must not overflow when applying quantity
- configured pressure bounds must contain `0`
- projected segment price must stay within `[minUnitPrice, maxUnitPrice]`

Execution-level:

- quoted total equals deterministic traversal total
- executed quantity equals requested quantity
- quote tokens remain single-use
- failed settlement does not mutate market pressure
- successful buy increases `netPosition`
- successful sell decreases `netPosition`
- hard bounds reject before settlement
- regeneration only moves `netPosition` toward `0`

Snapshot-level:

- `snapshotVersion` changes when authoritative state or trade-affecting config changes
- pressure fields are derived from `netPosition`
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
- buy rejects configured `maxNetPosition`
- sell rejects configured `minNetPosition`
- pressure mutation overflow rejection

Integration tests:

- snapshot exposes pressure fields
- snapshot does not require finite stock semantics
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

- default catalog creates valid pressure config
- legacy finite segment state maps to deterministic `netPosition`
- invalid legacy segment state fails or is marked for manual repair
- migrated snapshot matches the new pressure contract
