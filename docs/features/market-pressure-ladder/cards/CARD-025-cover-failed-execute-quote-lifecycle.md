# CARD-025: Cover Failed Execute Quote Lifecycle

## Status

implemented

## Objective

Add focused test coverage for quote lifecycle and no-mutation behavior when a claimed market execute fails.

## Context

Audit evidence confirmed that market execute claims a quote before applying locked item settlement. Existing pressure-ladder cards intentionally preserve single-use quote semantics after post-consume stale or failed settlement paths.

This card does not change that contract. It adds explicit regression coverage so later changes do not accidentally create trade history, mutate balances, mutate pressure state, or make quote lifecycle behavior ambiguous after a failed execute attempt.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../market-trade-history/contract.md`
- `CARD-015-reject-post-consume-plan-mismatch-as-stale-quote.md`

## Expected Behavior

Failed execute attempts after quote claim preserve the existing single-use quote lifecycle and do not write successful trade side effects.

The tests document the current expected behavior rather than changing market execution semantics.

## Acceptance Criteria

- [ ] A post-claim stale quote execution test proves no balance mutation occurs.
- [ ] A post-claim stale quote execution test proves no market pressure mutation occurs.
- [ ] A post-claim stale quote execution test proves no market trade history record is written.
- [ ] A failed settlement path, such as insufficient funds after quote claim, proves the quote remains non-active according to current single-use semantics.
- [ ] Replaying the same quote after the failed attempt returns the existing stale/non-active quote rejection.
- [ ] Existing successful execute tests remain unchanged.

## Expected Files to Change

```text
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
```

## Constraints

- Do not change quote token format.
- Do not change quote single-use semantics.
- Do not change stale quote, expired quote, or insufficient-funds rejection codes.
- Do not change market pressure mutation rules.
- Do not change trade-history write behavior except to verify existing no-write guarantees.
- Do not implement retryable settlement semantics in this card.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Changing quote consumption transaction boundaries.
- Changing failed-settlement retry behavior.
- Changing public market API response shapes.
- Changing balance settlement implementation.
- Changing market trade-history schema.

## Completion Notes

- Added failed execute lifecycle assertions for no trade-history writes, no balance mutation, no pressure mutation, consumed quote state, and stale replay behavior.
- Preserved existing single-use quote semantics after post-claim stale and insufficient-funds failures.
- Validation passed with the card command from `java/`.
