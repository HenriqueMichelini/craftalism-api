# CARD-005: Replace Quote Planning Traversal

## Status

completed

## Objective

Make quote planning walk virtual pressure positions instead of persisted segments.

## Context

Source: audit findings 4 and 9.

`craftalism-api` owns quote totals and rejection semantics.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Quote planning prices requested quantities over virtual pressure positions and rejects only configured hard pressure bounds.

## Acceptance Criteria

- [x] BUY prices positions `netPosition` through `netPosition + quantity - 1`.
- [x] SELL prices positions `netPosition - 1` through `netPosition - quantity`.
- [x] `INSUFFICIENT_STOCK` is emitted only for configured pressure bounds.
- [x] Ordinary buys and sells are not limited by finite stock.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/MarketRejectionCode.java
java/src/test/...
```

## Constraints

- Do not mutate `netPosition` in execute yet.
- Do not update snapshot contract yet.
- Expect legacy quote-total tests to change only where they assert superseded behavior.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Include planner unit tests for crossing segment boundaries in both directions, bounds, and overflow.

## Out of Scope

- Execute mutation.
- Snapshot contract changes.

## Suggested Commit Message

`feat(craftalism-api): plan quotes over pressure positions`

## Completion Notes

- Implemented quote planning over virtual pressure positions for BUY and SELL.
- Preserved legacy segment deltas only for existing out-of-scope mutation paths; quote totals and full-plan acceptance no longer depend on finite stock.
- Updated tests for pressure traversal boundaries, configured pressure bounds, overflow, and superseded finite-stock quote behavior.
- Validation: `rtk ./gradlew test` from `java/` passed.
