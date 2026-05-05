# craftalism-market Pressure Ladder Change Log

## Purpose

This document is a handoff note for implementing the market pressure-ladder contract in `craftalism-market`.

`craftalism-api` owns the authoritative contract and backend behavior. `craftalism-market` consumes snapshots, requests quotes, executes quote-backed trades, and displays backend-provided market state without recalculating prices locally.

Authoritative backend references:

- `docs/market-pressure-ladder-sigmoid-pricing.md`
- `docs/market-contract-mvp.md`

---

## Contract-Breaking Snapshot Change

`currentStock` is removed from the target market snapshot contract.

Replace all client usage of `currentStock` with:

```text
marketPressure: signed long
marketSegment: signed long
pressureMagnitude: non-negative long
```

Field meanings:

- `marketPressure`: signed backend pressure position
- `marketSegment`: signed pressure segment derived by the backend
- `pressureMagnitude`: absolute pressure value for display, sorting, or filtering

Client code must not treat these fields as inventory.

---

## Snapshot Item Shape

Target snapshot item:

```json
{
  "itemId": "wheat",
  "displayName": "Wheat",
  "iconKey": "WHEAT",
  "buyUnitEstimate": "50000",
  "sellUnitEstimate": "48039",
  "currency": "coins",
  "marketPressure": -25,
  "marketSegment": -1,
  "pressureMagnitude": 25,
  "variationPercent": "-3.92",
  "blocked": false,
  "operating": true,
  "lastUpdatedAt": "2026-04-12T18:29:42Z"
}
```

Remove expectations for:

```text
currentStock
```

---

## Display Semantics

Recommended client display rules:

- `marketPressure > 0`: demand pressure
- `marketPressure == 0`: balanced
- `marketPressure < 0`: oversupply pressure

Use `pressureMagnitude` when a non-negative display value is needed.

Use `marketSegment` for tier or ladder indicators.

Avoid inventory language such as stock, available, remaining, depleted, or sold out unless a future API explicitly exposes a hard pressure bound as inventory-like information.

---

## Pricing Rules For Clients

No client-side price calculation changes are required because clients must not calculate authoritative prices.

Continue to:

- display `buyUnitEstimate` and `sellUnitEstimate` as informational estimates
- request quotes for quantity-sensitive prices
- display quote `unitPrice` and `totalPrice` as authoritative for that quote
- execute trades with the returned `quoteToken`
- treat `snapshotVersion` and `quoteToken` as opaque

Do not derive totals from:

- `marketPressure`
- `marketSegment`
- `pressureMagnitude`
- `buyUnitEstimate`
- `sellUnitEstimate`

---

## Execute Response Change

Successful execute responses still include `updatedItem`, but `updatedItem` uses the new pressure fields instead of `currentStock`.

Target `updatedItem`:

```json
{
  "itemId": "wheat",
  "displayName": "Wheat",
  "iconKey": "WHEAT",
  "buyUnitEstimate": "50000",
  "sellUnitEstimate": "50000",
  "currency": "coins",
  "marketPressure": 7,
  "marketSegment": 0,
  "pressureMagnitude": 7,
  "variationPercent": "0",
  "blocked": false,
  "operating": true,
  "lastUpdatedAt": "2026-04-12T18:31:05Z"
}
```

---

## Rejection Semantics

`INSUFFICIENT_STOCK` remains a valid rejection code, but its meaning changes.

New meaning:

- buy would exceed configured `maxNetPosition`
- sell would go below configured `minNetPosition`

It no longer means ordinary finite stock exhaustion.

Client copy should avoid saying the market is out of stock. Prefer wording such as:

```text
The market cannot fill that quantity right now.
```

---

## Client Implementation Checklist

1. Update snapshot DTO/parser fields.
2. Remove `currentStock` reads from item rendering, sorting, filtering, and tests.
3. Add `marketPressure`, `marketSegment`, and `pressureMagnitude` to item models.
4. Update market item UI copy away from inventory/stock wording.
5. Update execute response handling for `updatedItem`.
6. Update rejection-code copy for `INSUFFICIENT_STOCK`.
7. Keep quote and execute request flows unchanged.
8. Keep treating quote totals, `snapshotVersion`, and `quoteToken` as opaque backend values.
9. Update fixtures and contract tests.
10. Add UI tests for positive, zero, and negative pressure display.

---

## Suggested Test Fixtures

Balanced:

```json
{
  "marketPressure": 0,
  "marketSegment": 0,
  "pressureMagnitude": 0
}
```

Demand pressure:

```json
{
  "marketPressure": 125,
  "marketSegment": 2,
  "pressureMagnitude": 125
}
```

Oversupply pressure:

```json
{
  "marketPressure": -25,
  "marketSegment": -1,
  "pressureMagnitude": 25
}
```

Boundary behavior:

```json
[
  { "marketPressure": 49, "marketSegment": 0, "pressureMagnitude": 49 },
  { "marketPressure": 50, "marketSegment": 1, "pressureMagnitude": 50 },
  { "marketPressure": -1, "marketSegment": -1, "pressureMagnitude": 1 },
  { "marketPressure": -50, "marketSegment": -1, "pressureMagnitude": 50 },
  { "marketPressure": -51, "marketSegment": -2, "pressureMagnitude": 51 }
]
```
