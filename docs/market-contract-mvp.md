# Market Contract MVP

## Purpose

This document defines the minimum authoritative market contract that `craftalism-api` must expose so `craftalism-market` can implement quote-aware trading without guessing backend semantics.

`craftalism-api` owns:

- snapshot payloads
- quote payloads
- execute payloads
- rejection codes
- `snapshotVersion`
- `quoteToken`
- blocked/operating semantics
- pressure-ladder market semantics

`craftalism-market` consumes this contract and must not redefine it locally.

This contract follows `docs/market-pressure-ladder-sigmoid-pricing.md`.

---

## Core Rules

- Snapshot prices are informational only.
- Quotes and execute responses are authoritative.
- Clients must not compute authoritative totals locally.
- Clients must treat `snapshotVersion` and `quoteToken` as opaque values.
- Rejections must use stable machine-readable codes.
- `currentStock` is not part of the pressure-ladder snapshot contract.

---

## Snapshot Contract

Endpoint:

```text
GET /api/market/snapshot
```

Response:

```json
{
  "snapshotVersion": "opaque-version-token",
  "generatedAt": "2026-04-12T18:30:00Z",
  "categories": [
    {
      "categoryId": "farming",
      "displayName": "Farming",
      "items": [
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
      ]
    }
  ]
}
```

Required semantics:

- `snapshotVersion`: opaque stale-detection token.
- `buyUnitEstimate`: display-only price estimate for the next buy unit.
- `sellUnitEstimate`: display-only price estimate for the next sell unit.
- `marketPressure`: signed backend pressure position.
- `marketSegment`: signed pressure segment derived from `marketPressure`.
- `pressureMagnitude`: non-negative absolute pressure value for display, sorting, or filtering.
- `blocked`: item cannot be traded if true.
- `operating`: item is not currently tradable if false.

Pressure display semantics:

- `marketPressure > 0`: demand pressure.
- `marketPressure == 0`: balanced.
- `marketPressure < 0`: oversupply pressure.

---

## Quote Contract

Endpoint:

```text
POST /api/market/quotes
```

Request:

```json
{
  "itemId": "wheat",
  "side": "BUY",
  "quantity": 32,
  "snapshotVersion": "opaque-version-token",
  "playerUuid": "220e8400-e29b-41d4-a716-446655440000"
}
```

`playerUuid` is optional and is honored only for the configured trusted Minecraft server client. Trusted clients may alternatively send the same value in `X-Craftalism-Player-Uuid`.

Response:

```json
{
  "itemId": "wheat",
  "side": "BUY",
  "quantity": 32,
  "unitPrice": "50000",
  "totalPrice": "1600000",
  "currency": "coins",
  "quoteToken": "opaque-quote-token",
  "snapshotVersion": "opaque-version-token",
  "expiresAt": "2026-04-12T18:31:14Z",
  "blocked": false,
  "operating": true
}
```

Required semantics:

- `quoteToken`: opaque token required for execute.
- `snapshotVersion`: authoritative state associated with the quote.
- `expiresAt`: quote expiry.
- `unitPrice` and `totalPrice`: authoritative for this quote only.

---

## Execute Contract

Endpoint:

```text
POST /api/market/execute
```

Request:

```json
{
  "itemId": "wheat",
  "side": "BUY",
  "quantity": 32,
  "quoteToken": "opaque-quote-token",
  "snapshotVersion": "opaque-version-token",
  "playerUuid": "220e8400-e29b-41d4-a716-446655440000"
}
```

`playerUuid` is optional and is honored only for the configured trusted Minecraft server client. Trusted clients may alternatively send the same value in `X-Craftalism-Player-Uuid`.

Success response:

```json
{
  "status": "SUCCESS",
  "itemId": "wheat",
  "side": "BUY",
  "executedQuantity": 32,
  "unitPrice": "50000",
  "totalPrice": "1600000",
  "currency": "coins",
  "snapshotVersion": "opaque-version-token",
  "updatedItem": {
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
}
```

Required semantics:

- execute is single-use for a given `quoteToken`.
- the first execute attempt claims the quote before final settlement checks run.
- later retries with the same `quoteToken` must reject with `STALE_QUOTE`.
- settlement is not retried once the quote has been claimed.
- a quote may remain `CONSUMED` even when settlement returns a business rejection such as `INSUFFICIENT_FUNDS`.
- successful buy increases `marketPressure`.
- successful sell decreases `marketPressure`.

---

## Trade History Contract

Trade history is API-side operational history for completed market executions. It is not a quote feed, but it follows the current MVP public read policy for `GET /api/**`.

List endpoint:

```text
GET /api/market/trades
```

Detail endpoint:

```text
GET /api/market/trades/{id}
```

Access:

- Public read; no bearer token is required.
- Write-side market operations remain protected separately by the quote and execute scope rules.

List filters:

- `playerUuid`
- `itemId`
- `side`
- `executedFrom`
- `executedTo`

List responses use the repository-standard Spring `Page<MarketTradeHistoryDTO>` JSON shape and support Spring Data pageable query parameters: zero-based `page`, `size`, and repeated `sort=property,direction` values. The default ordering is newest first by `executedAt,DESC`, then `id,DESC` for deterministic ordering when multiple trades share the same execution timestamp.

`executedFrom` and `executedTo` are inclusive instant bounds.

Trade history record:

```json
{
  "id": 123,
  "playerUuid": "220e8400-e29b-41d4-a716-446655440000",
  "itemId": "wheat",
  "side": "BUY",
  "quantity": 32,
  "unitPrice": "50000",
  "totalPrice": "1600000",
  "currency": "coins",
  "snapshotVersion": "opaque-version-token",
  "executedAt": "2026-04-12T18:31:05Z"
}
```

Required semantics:

- A trade history record represents one committed successful `/api/market/execute`.
- The record is persisted in the same transaction as the successful execute after the trade is applied.
- Rejected attempts, pending quotes, expired quotes, and quote lifecycle records are not trade history.
- Read endpoints expose only committed successful executions.
- `snapshotVersion` remains opaque to clients.
- Monetary values use the same string-encoded integer representation as quote and execute responses.

---

## Rejection Contract

Response:

```json
{
  "status": "REJECTED",
  "code": "STALE_QUOTE",
  "message": "Quote is no longer valid.",
  "snapshotVersion": "opaque-version-token"
}
```

Required codes:

- `STALE_QUOTE`
- `ITEM_BLOCKED`
- `ITEM_NOT_OPERATING`
- `INSUFFICIENT_STOCK`
- `INSUFFICIENT_FUNDS`
- `MARKET_CLOSED`
- `INVALID_QUANTITY`
- `RATE_LIMITED`
- `QUOTE_EXPIRED`
- `API_UNAVAILABLE`
- `UNKNOWN_ITEM`

`INSUFFICIENT_STOCK` is emitted only when a configured hard pressure bound prevents full execution:

- buy would exceed `maxNetPosition`
- sell would go below `minNetPosition`

It is not emitted for ordinary pressure-ladder trading.

---

## Opaque Token Rules

### `snapshotVersion`

- compare-only stale token
- clients must not parse meaning from it
- formatted by the backend as `market:<hash>`, but clients must treat that shape as opaque
- derived from authoritative pressure state and trade-affecting pressure config
- changes when fields such as `marketPressure`, hard pressure bounds, blocked/operating state, item update time, unit price bounds, segment size, pressure sensitivity, or regeneration config change
- does not change for display-only recalculations such as estimates, legacy `currentStock`, legacy segment rows, market momentum, or variation percent

### `quoteToken`

- compare/pass-through token for execute
- clients must not inspect or modify it
- may expire or become invalid if state changes

---

## Client Rules

`craftalism-market` may:

- browse with snapshots
- request quotes for quantity-sensitive pricing
- execute using quote-backed requests
- map rejection codes to player-facing messages
- display pressure direction and magnitude

`craftalism-market` must not:

- compute authoritative totals locally
- infer backend behavior from token structure
- treat pressure fields as inventory
- use inventory language unless a configured hard pressure bound is exposed separately
