# CARD-005: Move Transfer Services to Feature Package

## Status

completed

## Objective

Mechanically move `TransferService` and `TransferIncidentService` from the generic service package into the transfer application package.

## Context

The service-layer package audit confirmed that `TransferService` and `TransferIncidentService` form the isolated transfer application boundary. Their current placement under `api.service` hides that feature boundary without providing useful package cohesion.

This card is a package reorganization only. It must preserve canonical transfer, idempotency, ledger, and incident behavior.

## Required Reading

- `../contract.md`

## Expected Behavior

Transfer and incident endpoints, atomic settlement behavior, idempotency, ledger persistence, incident recording, error semantics, permissions, and transaction boundaries remain unchanged while both services are located under `io.github.HenriqueMichelini.craftalism.api.transfer.application`.

## Acceptance Criteria

- [ ] `TransferService` and `TransferIncidentService` are moved to `io.github.HenriqueMichelini.craftalism.api.transfer.application`.
- [ ] Class names and public visibility remain unchanged.
- [ ] Production and test imports reference the new package.
- [ ] No transfer behavior, DTO, endpoint, repository, schema, permission, idempotency, ledger, incident, or transaction-boundary behavior changes.
- [ ] Focused transfer tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransferService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransferIncidentService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/transfer/application/TransferService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/transfer/application/TransferIncidentService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceController.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/TransferIncidentController.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceTransferIntegrationTest.java
```

## Constraints

- Do not rename either service.
- Do not change service visibility unless compilation requires the smallest possible adjustment.
- Do not change transfer behavior, public contracts, or test expectations.
- Do not move balance, market, or table-filter classes.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceTransferIntegrationTest
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Moving player, balance, transaction, market, or table-filter classes.
- Renaming services.
- Changing transfer idempotency, ledger, incident, or API behavior.
- Refactoring transfer orchestration.

## Suggested Commit Message

`refactor(api): move transfer services to feature package`

## Completion Notes

- Moved `TransferService` and `TransferIncidentService` into the transfer
  application package and updated directly related imports.
- Verified with the declared focused Gradle tests and the final full Gradle
  test task.
