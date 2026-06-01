# CARD-004: Move Balance Service to Feature Package

## Status

completed

## Objective

Mechanically move `BalanceService` from the generic service package into the wallet application package.

## Context

The service-layer package audit confirmed that `BalanceService` is an isolated wallet application service. Its current placement under `api.service` hides the feature boundary without providing useful package cohesion.

This card is a package reorganization only. It must preserve existing balance behavior and public interfaces.

## Required Reading

- `../contract.md`

## Expected Behavior

Balance endpoints, service behavior, validation, persistence, error semantics, and transaction boundaries remain unchanged while `BalanceService` is located under `io.github.HenriqueMichelini.craftalism.api.wallet.application`.

## Acceptance Criteria

- [ ] `BalanceService` is moved to `io.github.HenriqueMichelini.craftalism.api.wallet.application`.
- [ ] The class name and public visibility remain unchanged.
- [ ] Production and test imports reference the new package.
- [ ] `BalanceServiceTest` is moved to the matching feature-oriented test package.
- [ ] No balance behavior, DTO, endpoint, repository, schema, permission, or transaction-boundary behavior changes.
- [ ] Focused balance tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/wallet/application/BalanceService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceController.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/wallet/application/BalanceServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceControllerTest.java
```

## Constraints

- Do not rename `BalanceService`.
- Do not change service visibility unless compilation requires the smallest possible adjustment.
- Do not change balance behavior, public contracts, or test expectations.
- Do not move transfer, market, or table-filter classes.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.wallet.application.BalanceServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceControllerTest
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Moving player, transaction, transfer, market, or table-filter classes.
- Renaming services.
- Changing balance or transfer API behavior.
- Refactoring balance validation or persistence logic.

## Suggested Commit Message

`refactor(api): move balance service to wallet package`

## Completion Notes

- Moved `BalanceService` and `BalanceServiceTest` into the wallet application
  package and updated directly related imports.
- Verified with the declared focused Gradle tests and the final full Gradle
  test task.
