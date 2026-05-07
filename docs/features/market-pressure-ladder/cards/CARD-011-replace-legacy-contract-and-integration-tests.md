# CARD-011: Replace Legacy Contract And Integration Tests

## Status

completed

## Objective

Align unit, integration, and contract tests with pressure-ladder behavior.

## Context

Source: audit finding 11.

`craftalism-api` owns backend test confidence for market contracts.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Tests assert pressure-ladder behavior instead of superseded stock and persisted-segment runtime semantics.

## Acceptance Criteria

- [x] Tests cover segment derivation for positive, zero, and negative pressure.
- [x] Tests cover quote traversal across positive and negative virtual segments.
- [x] Tests cover snapshot pressure fields and absence of target `currentStock`.
- [x] Tests cover hard-bound `INSUFFICIENT_STOCK`, stale quote, single-use quote, and failed settlement no mutation.
- [x] Tests cover migration and backfill consistency.

## Expected Files to Change

```text
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlannerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketReadServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketBootstrapPerformanceIntegrationTest.java
java/src/test/...
```

## Constraints

- Do not preserve tests that assert superseded stock semantics.
- Do not add client-side tests in this repository.
- Best done after behavior cards, or incrementally alongside each behavior card if keeping the suite green per commit.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Client-side tests.
- Preserving superseded stock-semantics assertions.

## Suggested Commit Message

`test(craftalism-api): cover pressure ladder market behavior`

## Completion Notes

Implemented on 2026-05-06.

- Added pressure-ladder test coverage for negative-boundary buy traversal and direct positive, zero, and negative snapshot pressure projection.
- Replaced legacy-shaped market service test setup with pressure-state setup and added minimum-pressure sell bound rejection coverage.
- Strengthened failed-settlement tests to assert balance and market pressure remain unchanged.
- Added integration coverage for sell hard-bound `INSUFFICIENT_STOCK`.
- Validation passed with `rtk ./gradlew test` from `java/`.
