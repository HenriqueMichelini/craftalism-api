# Market Targeted Refresh Backlog

Date: 2026-04-12

## Ownership

`craftalism-api` owns the authoritative market refresh contract when the backend exposes one.

This includes:

- snapshot payload semantics
- targeted refresh payload semantics
- `snapshotVersion` behavior
- blocked/operating state semantics
- execute response authority

`craftalism-market` is a consumer and must not define authoritative refresh semantics locally.

## Current State

Current plugin behavior is compatible with the existing authoritative contract:

- `GET /api/market/snapshot`
- `POST /api/market/quotes`
- `POST /api/market/execute`

The plugin now performs repo-local post-trade refresh and stale/expired quote refresh handling without requiring any API contract change.

That behavior stays within the consumer boundary and is not a blocker for `craftalism-api`.

## Problem Framing

The current full snapshot refresh path is correct, but it may be broader than necessary after:

- successful trade settlement
- `STALE_QUOTE` rejection
- `QUOTE_EXPIRED` rejection

This is currently an efficiency and client-flow concern, not a correctness issue.

## Future Goal

If optimization is worth the added contract surface, `craftalism-api` may expose a narrower authoritative refresh path so consumers do not always need a full snapshot fetch.

## Candidate Approaches

### 1. Item-Scoped Refresh Endpoint

Example direction:

- `GET /api/market/items/{categoryId}/{itemId}`

Potential use:

- refresh the traded item after execute
- refresh a single stale or expired item view

Questions to resolve:

- whether `categoryId` is required in the route or only in the payload
- whether the response should contain only the item or item plus category metadata
- how unknown or unavailable items should be represented

### 2. Category-Scoped Refresh Endpoint

Example direction:

- `GET /api/market/categories/{categoryId}`

Potential use:

- refresh the currently open category after execute
- keep local GUI state aligned without loading the full market

Questions to resolve:

- whether category ordering remains authoritative in this payload
- whether category refresh is sufficient for all known client flows
- whether item-level refresh still needs to exist

### 3. Execute-As-Refresh Contract

Example direction:

- treat `POST /api/market/execute` success payload as sufficient authoritative refresh data for the affected item

Potential use:

- apply immediate local refresh from the execute response
- avoid an extra fetch after successful settlement

Questions to resolve:

- whether `updatedItem` is contractually sufficient for client refresh
- whether consumers still need category context after execute
- whether rejection responses should ever include more authoritative refresh data

## Contract Requirements To Define Before Implementation

If this work is taken up later, define all of the following explicitly:

- exact payload shape
- whether the contract is item-only or category-scoped
- whether multiple targeted refresh shapes can coexist
- whether `snapshotVersion` returned by targeted refresh is market-wide or scoped
- whether targeted refresh responses can advance stale detection for the whole market
- how blocked state is represented
- how non-operating state is represented
- how unknown or no-longer-visible items are represented
- whether execute responses alone are sufficient for immediate client refresh
- whether stale/expired quote handling should use the same refresh contract as success handling

## Boundary Rules

Preserve the current ownership split:

- `craftalism-api` remains authoritative for snapshot semantics, blocked/operating state, quote validity, execution results, and version semantics
- `craftalism-market` consumes backend state and updates local GUI/session state only

Do not move authoritative market interpretation into the plugin.

## Recommendation

Do not implement this yet unless profiling or user experience evidence shows the full snapshot refresh path is materially too expensive or too coarse for the current market flow.

If this becomes urgent, start with a contract design decision first, not an implementation-first change.
