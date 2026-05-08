# CARD-020: Extract Market Execute Orchestration

## Status

completed

## Objective

Move execute request orchestration and quote validation out of `MarketService`.

## Context

The execute path contributes significantly to `MarketService` size and coupling. Settlement is already delegated to `MarketTradeExecutor`, but quote lookup, quote status validation, player matching, snapshot checks, token consumption, item locking, and response assembly remain in `MarketService`.

This card preserves behavior while creating a focused execution orchestration boundary.

## Required Reading

- `../contract.md`

## Expected Behavior

Execute behavior remains unchanged, including single-use quote behavior, stale quote rejection, quote expiration, item locking, failed-settlement no-mutation behavior, and execute success response shape.

## Acceptance Criteria

- [ ] `MarketService.execute(...)` delegates execute orchestration to a focused component.
- [ ] Quote status, expiration, request mismatch, snapshot mismatch, and consume-failure paths preserve existing rejection codes and HTTP statuses.
- [ ] Balance settlement remains delegated to `MarketTradeExecutor`.
- [ ] Execute success response fields remain unchanged.
- [ ] Execute tests and market contract integration tests pass without behavior expectation changes.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketExecuteService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
```

## Constraints

- Do not change balance transfer semantics.
- Do not change quote single-use semantics.
- Do not change stale quote or quote expired semantics.
- Do not change market pressure mutation rules.
- Do not change public API DTOs or endpoint contracts.

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

- Quote creation flow.
- Snapshot projection flow.
- Public API or DTO changes.
- Database schema changes.

## Suggested Commit Message

`refactor(craftalism-api): extract market execute orchestration`

## Completion Notes

- Extracted execute orchestration into `MarketExecuteService`.
- `MarketService.execute(...)` now delegates to the focused execute service.
- Preserved quote validation, expiration, stale handling, rate limiting, item locking, response assembly, and settlement delegation to `MarketTradeExecutor`.
- Validation passed from `java/`: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest`.
