# CARD-003: Reject Balance Arithmetic Overflow

## Status

implemented

## Objective

Reject balance mutation arithmetic overflow before persistence can store or attempt invalid authoritative balances.

## Context

Audit evidence confirmed balance credit paths use raw `long` arithmetic:

- `BalanceService.deposit(...)` adds request amount to the current balance.
- `TransferService.executeAtomicTransfer(...)` credits the destination balance.
- `MarketTradeExecutor.applySell(...)` credits market sell proceeds.

The balance integrity contract requires balances to remain non-negative across service and persistence boundaries. Raw `long` addition can overflow to a negative or otherwise incorrect amount before the database `CHECK (amount >= 0)` safeguard runs.

Depends on: `CARD-002-retire-duplicate-balance-service-transfer-path.md`.

## Required Reading

- `../contract.md`
- `../../../repo-contract-map.md`
- `../../../repo-requirement-pack.md`
- `../../market-pressure-ladder/contract.md`
- `CARD-002-retire-duplicate-balance-service-transfer-path.md`

## Expected Behavior

Balance deposit, canonical transfer credit, and market sell settlement reject arithmetic overflow with a stable domain error before saving mutated balances.

Existing valid balance mutations continue to succeed. Existing insufficient-funds behavior remains unchanged.

## Acceptance Criteria

- [ ] `BalanceService.deposit(...)` rejects overflow before `repository.save(...)`.
- [ ] `TransferService.transfer(...)` rejects destination credit overflow without persisting partial debit, credit, or transaction ledger state.
- [ ] Market sell settlement rejects balance credit overflow without persisting market pressure mutation or trade history.
- [ ] Existing insufficient-funds and invalid-amount behavior remains unchanged.
- [ ] Overflow rejection maps to an existing or explicitly scoped domain error, not a generic 500.
- [ ] Focused service or integration tests cover deposit overflow, transfer credit overflow, and market sell credit overflow.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransferService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceTransferIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
```

## Constraints

- Do not change public balance, transfer, market quote, or market execute DTO shapes.
- Do not change successful settlement math for valid amounts.
- Do not change transfer idempotency replay or conflict semantics.
- Do not change market pressure-ladder pricing math.
- Do not introduce floating-point or decimal balance storage.
- Do not change database schema unless implementation proves a persistence constraint is missing.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.BalanceServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceTransferIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Changing amount scale or currency model.
- Adding maximum balance product policy beyond overflow rejection.
- Reworking transfer idempotency storage.
- Reworking market quote lifecycle.
- Removing duplicate transfer paths; use the prerequisite card for that.

## Completion Notes

- Added explicit overflow rejection before balance credit persistence in deposit, canonical transfer credit, and market sell settlement.
- Added focused overflow coverage for deposit, canonical transfer, market executor, and market execute integration behavior.
- Market sell overflow now returns a stable `BALANCE_OVERFLOW` market rejection and preserves no-mutation guarantees.
- Validation passed with the card command from `java/`.
