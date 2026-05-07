# CARD-006: Mutate NetPosition During Execute

## Status

completed

## Objective

Update execute settlement to mutate `netPosition` after quote verification and successful settlement.

## Context

Source: audit finding 5.

`craftalism-api` owns trade execution and market mutation.

## Required Reading

- `../contract.md`

## Expected Behavior

Successful execute mutates pressure state after quote validation and settlement; failed settlement leaves pressure unchanged.

## Acceptance Criteria

- [x] BUY increments `netPosition` by quantity.
- [x] SELL decrements `netPosition` by quantity.
- [x] Failed settlement does not mutate pressure.
- [x] Rebuilt plan must match stored quote total and unit price before mutation.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/test/...
```

## Constraints

- Do not alter public snapshot fields in this card.
- Do not remove legacy segment entities yet.
- Preserve quote single-use semantics.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Include execute tests for buy, sell, insufficient funds, stale quote, and single-use quote.

## Out of Scope

- Snapshot DTO changes.
- Legacy segment entity removal.

## Suggested Commit Message

`feat(craftalism-api): execute trades against market pressure`

## Completion Notes

- Mutated `netPosition` after quote plan verification and successful balance settlement in execute.
- Preserved legacy segment mutation paths while keeping pressure mutation scoped to successful BUY/SELL execution.
- Added execute coverage for BUY, SELL, insufficient funds, stale quote, and single-use quote behavior.
- Validation: `rtk ./gradlew test` from `java/` passed.
