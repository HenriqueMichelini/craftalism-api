# CARD-002: Record Successful Market Executions

## Status

completed

## Objective

Persist a market trade history record for each successful `/api/market/execute`.

## Context

Trade history must expose committed successful executions only. The write must happen inside the successful execute transaction after settlement and market pressure mutation have succeeded.

Depends on: `CARD-001-add-market-trade-history-persistence.md`.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../market-pressure-ladder/contract.md`
- `CARD-001-add-market-trade-history-persistence.md`

## Expected Behavior

Successful market execute writes one immutable trade history record with the executed player, item, side, quantity, unit price, total price, currency, snapshot version, and execution timestamp. Rejected, expired, stale, rate-limited, and failed-settlement attempts write no trade history record.

## Acceptance Criteria

- [ ] Successful `/api/market/execute` persists exactly one trade history record in the same transaction as the applied trade.
- [ ] The record is saved only after settlement and market state mutation succeed.
- [ ] Rejected execute responses do not create trade history records.
- [ ] Expired quotes, stale quotes, pending quotes, rejected attempts, and consumed-with-failed-settlement quote states are excluded from trade history.
- [ ] Existing execute response shape and rejection semantics remain unchanged.
- [ ] Service or integration tests prove successful writes and no writes for rejected or failed execute paths.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketExecuteService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketTradeHistoryRepository.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
```

## Constraints

- Do not change public execute DTOs or endpoint paths.
- Do not change balance transfer semantics.
- Do not change quote single-use, stale quote, quote expiration, or failed-settlement behavior.
- Do not expose read endpoints in this card.
- Do not backfill existing quotes into trade history.

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

- Trade history list or detail endpoints.
- Dashboard or client changes.
- Reworking quote persistence.
- Retrying settlement after quote consumption.

## Suggested Commit Message

`feat(craftalism-api): record successful market executions`

## Completion Notes

Successful market execute now writes one trade-history record after balance settlement and market state mutation; rejected and failed execution paths do not save trade history.
