# Market Contract MVP

  ## Purpose

  This document defines the minimum authoritative market contract that `craftalism-api` must expose so
  `craftalism-market` can implement quote-aware trading without guessing backend semantics.

  `craftalism-api` owns:
  - snapshot payloads
  - quote payloads
  - execute payloads
  - rejection codes
  - `snapshotVersion`
  - `quoteToken`
  - blocked/operating semantics

  `craftalism-market` consumes this contract and must not redefine it locally.

  ---

  ## Core Rules

  - Snapshot prices are informational only.
  - Quotes and execute responses are authoritative.
  - Clients must not compute authoritative totals locally.
  - Clients must treat `snapshotVersion` and `quoteToken` as opaque values.
  - Rejections must use stable machine-readable codes.

  ---

  ## Snapshot Contract

  ## Endpoint

  `GET /api/market/snapshot`

  ## Response

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
            "buyUnitEstimate": "5",
            "sellUnitEstimate": "4",
            "currency": "coins",
            "currentStock": 1820,
            "variationPercent": "2.3",
            "blocked": false,
            "operating": true,
            "lastUpdatedAt": "2026-04-12T18:29:42Z"
          }
        ]
      }
    ]
  }

  ## Required Semantics

  - snapshotVersion: opaque stale-detection token
  - buyUnitEstimate / sellUnitEstimate: display-only estimates
  - blocked: item cannot be traded
  - operating: item is not currently tradable if false

  ———

  ## Quote Contract

  ## Endpoint

  `POST /api/market/quotes`

  ## Request

  {
    "itemId": "wheat",
    "side": "BUY",
    "quantity": 32,
    "snapshotVersion": "opaque-version-token"
  }

  ## Response

  {
    "itemId": "wheat",
    "side": "BUY",
    "quantity": 32,
    "unitPrice": "5",
    "totalPrice": "160",
    "currency": "coins",
    "quoteToken": "opaque-quote-token",
    "snapshotVersion": "opaque-version-token",
    "expiresAt": "2026-04-12T18:31:14Z",
    "blocked": false,
    "operating": true
  }

  ## Required Semantics

  - quoteToken: opaque token required for execute
  - snapshotVersion: authoritative state associated with the quote
  - expiresAt: quote expiry
  - unitPrice / totalPrice: authoritative for this quote only

  ———

  ## Execute Contract

  ## Endpoint

  `POST /api/market/execute`

  ## Request

  {
    "itemId": "wheat",
    "side": "BUY",
    "quantity": 32,
    "quoteToken": "opaque-quote-token",
    "snapshotVersion": "opaque-version-token"
  }

  ## Success Response

  {
    "status": "SUCCESS",
    "itemId": "wheat",
    "side": "BUY",
    "executedQuantity": 32,
    "unitPrice": "5",
    "totalPrice": "160",
    "currency": "coins",
    "snapshotVersion": "opaque-version-token",
    "updatedItem": {
      "itemId": "wheat",
      "displayName": "Wheat",
      "iconKey": "WHEAT",
      "buyUnitEstimate": "6",
      "sellUnitEstimate": "5",
      "currency": "coins",
      "currentStock": 1788,
      "variationPercent": "2.8",
      "blocked": false,
      "operating": true,
      "lastUpdatedAt": "2026-04-12T18:31:05Z"
    }
  }

  ## Required Semantics

  - execute is single-use for a given `quoteToken`
  - the first execute attempt claims the quote before final settlement checks run
  - later retries with the same `quoteToken` must reject with `STALE_QUOTE`
  - settlement is not retried once the quote has been claimed
  - a quote may remain `CONSUMED` even when settlement returns a business rejection such as `INSUFFICIENT_FUNDS`

  ———

  ## Rejection Contract

  ## Response

  {
    "status": "REJECTED",
    "code": "STALE_QUOTE",
    "message": "Quote is no longer valid.",
    "snapshotVersion": "opaque-version-token"
  }

  ## Required Codes

  - STALE_QUOTE
  - ITEM_BLOCKED
  - ITEM_NOT_OPERATING
  - INSUFFICIENT_STOCK
  - INSUFFICIENT_FUNDS
  - MARKET_CLOSED
  - INVALID_QUANTITY
  - RATE_LIMITED
  - QUOTE_EXPIRED
  - API_UNAVAILABLE
  - UNKNOWN_ITEM

  ———

  ## Opaque Token Rules

  ## snapshotVersion

  - compare-only stale token
  - clients must not parse meaning from it
  - may change whenever authoritative market state changes

  ## quoteToken

  - compare/pass-through token for execute
  - clients must not inspect or modify it
  - may expire or become invalid if state changes

  ———

  ## Client Rules

  craftalism-market may:

  - browse with snapshots
  - request quotes for quantity-sensitive pricing
  - execute using quote-backed requests
  - map rejection codes to player-facing messages

  craftalism-market must not:

  - compute authoritative totals locally
  - infer backend behavior from token structure
  - rely on free-form error text

  ———

  ## Minimum Open Questions To Resolve

  Confirmed for the MVP implementation:

  - Prices are encoded as strings containing authoritative whole-coin amounts.
  - `snapshotVersion` is a market-wide opaque token.
  - `quoteToken` is mandatory for every execute request.
  - Execute is single-use per `quoteToken`.
  - The first execute attempt claims the quote before final settlement checks run.
  - Retries with the same `quoteToken` are not guaranteed to re-run settlement.
  - A claimed quote may remain consumed even when settlement ends in a business rejection such as `INSUFFICIENT_FUNDS`.
  - `updatedItem` is always present on successful execute responses.
  - `blocked` and `operating` are both required:
    - `blocked` means the item is administratively unavailable.
    - `operating` means the market is temporarily not trading that item.
  - Business rejections return the rejection payload with an HTTP business status:
    - `409` for stale/expired/conflict-style rejections
    - `422` for quantity, stock, and funds rejections
    - `404` for `UNKNOWN_ITEM`
    - `503` for `MARKET_CLOSED` and `API_UNAVAILABLE`
  - The authenticated player actor is resolved from JWT `player_uuid` when present, otherwise from a UUID-valued `sub` claim.
