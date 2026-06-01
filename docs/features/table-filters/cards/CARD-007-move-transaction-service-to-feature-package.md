# CARD-007: Move Transaction Service to Feature Package

## Status

completed

## Objective

Mechanically move `TransactionService` from the generic service package into the transaction application package.

## Context

The service-layer package audit confirmed that `TransactionService` is an isolated transaction application service. Its current placement under `api.service` hides the feature boundary without providing useful package cohesion.

`TransactionService` currently uses package-private table-filter helpers. This card must not move those helpers. Apply only the smallest visibility adjustment required for compilation after moving the service.

## Required Reading

- `../contract.md`

## Expected Behavior

Transaction endpoints, recording, filtered reads, validation, pagination, sorting, response behavior, persistence, security, and transaction boundaries remain unchanged while `TransactionService` is located under `io.github.HenriqueMichelini.craftalism.api.transaction.application`.

## Acceptance Criteria

- [ ] `TransactionService` is moved to `io.github.HenriqueMichelini.craftalism.api.transaction.application`.
- [ ] The class name and public visibility remain unchanged.
- [ ] Production and test imports reference the new package.
- [ ] `TransactionServiceTest` is moved to the matching feature-oriented test package.
- [ ] `TableFilterSpecifications` and `TableFilterValidation` remain in their current package.
- [ ] Any visibility adjustment to table-filter helpers is the smallest change required for compilation.
- [ ] No transaction behavior, DTO, endpoint, repository, schema, permission, filter, sorting, pagination, or transaction-boundary behavior changes.
- [ ] Focused transaction tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/transaction/application/TransactionService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TableFilterSpecifications.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TableFilterValidation.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionController.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/transaction/application/TransactionServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionContractIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/security/SecurityFilterChainTest.java
```

## Constraints

- Do not rename `TransactionService`.
- Do not move or redesign table-filter helpers.
- Do not change service visibility unless compilation requires the smallest possible adjustment.
- Do not change transaction behavior, public contracts, filter semantics, or test expectations.
- Do not move unrelated services.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.transaction.application.TransactionServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.security.SecurityFilterChainTest
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Moving player, balance, transfer, market, or table-filter classes.
- Renaming services.
- Adding filters or changing pageable response behavior.
- Refactoring transaction mutation or query logic.

## Suggested Commit Message

`refactor(api): move transaction service to feature package`

## Completion Notes

- Moved `TransactionService` and `TransactionServiceTest` into the transaction
  application package.
- Updated controller and directly related test imports to reference the new
  package.
- Kept table-filter helpers in `api.service` and exposed only the helper
  classes and methods required by the moved service.
- Verified with the declared focused Gradle test command and the full Gradle
  test task.
