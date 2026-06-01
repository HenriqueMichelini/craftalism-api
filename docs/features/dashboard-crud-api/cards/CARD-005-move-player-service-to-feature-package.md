# CARD-005: Move Player Service to Feature Package

## Status

completed

## Objective

Mechanically move `PlayerService` from the generic service package into the player application package.

## Context

The service-layer package audit confirmed that `PlayerService` is an isolated player application service. Its current placement under `api.service` hides the feature boundary without providing useful package cohesion.

This card is a package reorganization only. It must preserve existing player behavior and public interfaces.

## Required Reading

- `../contract.md`

## Expected Behavior

Player endpoints, service behavior, validation, persistence, error semantics, and security behavior remain unchanged while `PlayerService` is located under `io.github.HenriqueMichelini.craftalism.api.player.application`.

## Acceptance Criteria

- [ ] `PlayerService` is moved to `io.github.HenriqueMichelini.craftalism.api.player.application`.
- [ ] The class name and public visibility remain unchanged.
- [ ] Production and test imports reference the new package.
- [ ] `PlayerServiceTest` is moved to the matching feature-oriented test package.
- [ ] No player behavior, DTO, endpoint, repository, schema, permission, or transaction-boundary behavior changes.
- [ ] Focused player tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/PlayerService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/player/application/PlayerService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/PlayerController.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/PlayerServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/player/application/PlayerServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/PlayerControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/security/SecurityFilterChainTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/exceptions/GlobalExceptionHandlerContratTest.java
```

## Constraints

- Do not rename `PlayerService`.
- Do not change service visibility unless compilation requires the smallest possible adjustment.
- Do not change player behavior, public contracts, or test expectations.
- Do not move unrelated services.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.player.application.PlayerServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.PlayerControllerTest --tests io.github.HenriqueMichelini.craftalism.api.security.SecurityFilterChainTest --tests io.github.HenriqueMichelini.craftalism.api.exceptions.GlobalExceptionHandlerContratTest
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Moving balance, transaction, transfer, market, or table-filter classes.
- Renaming services.
- Changing player API behavior.
- Refactoring player validation or persistence logic.

## Suggested Commit Message

`refactor(api): move player service to feature package`

## Completion Notes

- Moved `PlayerService` and `PlayerServiceTest` into the player application
  package and updated directly related imports.
- Verified with the declared focused Gradle tests and the full Gradle test task.
