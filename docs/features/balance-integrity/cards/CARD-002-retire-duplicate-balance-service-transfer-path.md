# CARD-002: Retire Duplicate Balance Service Transfer Path

## Status

implemented

## Objective

Remove the unused balance-only transfer path so transfers cannot bypass canonical idempotency, ledger, and incident semantics.

## Context

Audit evidence confirmed two backend transfer implementations:

- `TransferService.transfer(...)` handles idempotency, transaction ledger persistence, atomic balance mutation, and incident recording.
- `BalanceService.transfer(...)` mutates balances only and is currently referenced only by `BalanceServiceTest`.

The balance integrity contract requires transfer operations to remain atomic across debit, credit, ledger, idempotency, and incident handling behavior. Keeping an unused public service method that bypasses those guarantees creates an easy future regression path.

## Required Reading

- `../contract.md`
- `../../../repo-contract-map.md`
- `../../../repo-requirement-pack.md`

## Expected Behavior

Production transfer behavior remains routed through `TransferService.transfer(...)` and `POST /api/balances/transfer`.

No production service method remains available for balance-only transfer settlement that skips idempotency, transaction ledger persistence, or incident behavior.

## Acceptance Criteria

- [ ] `BalanceService.transfer(...)` is removed or made unavailable to production callers.
- [ ] No production source references `BalanceService.transfer(...)`.
- [ ] Existing `BalanceController.transfer(...)` continues to delegate to `TransferService.transfer(...)`.
- [ ] Canonical transfer idempotency, transaction ledger, and incident behavior remains unchanged.
- [ ] Balance service tests no longer cover a non-canonical balance-only transfer path.
- [ ] Transfer integration/controller tests continue to cover canonical transfer behavior.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceTransferIntegrationTest.java
```

## Constraints

- Do not change public transfer endpoint routes, request DTOs, response DTOs, or idempotency header behavior.
- Do not change transaction ledger persistence semantics.
- Do not weaken transfer incident recording.
- Do not change market settlement behavior.
- Do not perform unrelated balance service refactors.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.BalanceServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceTransferIntegrationTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Changing balance API shapes.
- Changing transfer idempotency semantics.
- Changing transfer error response shapes.
- Adding new transfer endpoints.
- Fixing arithmetic overflow; use the follow-up overflow card for that.

## Completion Notes

- Removed the duplicate `BalanceService.transfer(...)` production path.
- Removed balance-service unit coverage for the retired balance-only transfer path.
- Verified canonical transfer behavior still routes through `TransferService.transfer(...)`.
- Validation passed with the card command from `java/`.
