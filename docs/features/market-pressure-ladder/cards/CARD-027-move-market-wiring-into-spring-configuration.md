# CARD-027: Move Market Wiring Into Spring Configuration

## Status

completed

## Objective

Move manual market collaborator construction out of `MarketService` into focused Spring configuration while preserving the existing market facade.

## Context

`MarketService` delegates snapshot, quote, and execute behavior to focused collaborators, but its Spring constructor still accepts repositories, services, catalog data, and scalar configuration values before manually constructing the market object graph. This leaves the facade responsible for dependency wiring and runtime delegation.

This is an incremental wiring refactor, not a new architecture style.

## Required Reading

- `../contract.md`
- `../../market-events/contract.md`
- `CARD-018-extract-market-snapshot-orchestration.md`
- `CARD-019-extract-market-quote-orchestration.md`
- `CARD-020-extract-market-execute-orchestration.md`

## Expected Behavior

Spring creates the existing focused market collaborators through configuration, and `MarketService` remains a small facade with unchanged public methods and transaction behavior.

## Acceptance Criteria

- [ ] Market collaborator creation is moved from the `MarketService` constructor into a focused Spring `@Configuration` class.
- [ ] Market scalar settings used by the collaborator graph are grouped into a focused configuration-properties type or equivalent typed settings object.
- [ ] `MarketService` receives focused collaborators instead of repositories and scalar wiring inputs.
- [ ] Snapshot, quote, execute, catalog initialization, quote deletion, and active-quote-count behavior remain unchanged.
- [ ] Package-private collaborators remain internal implementation details unless Spring bean visibility requires a minimal change.
- [ ] Spring context startup and existing market behavior tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketQuoteService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketExecuteService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/ApplicationTests.java
```

## Constraints

- Do not change the layered architecture or introduce hexagonal or DDD infrastructure.
- Do not change public endpoints, DTOs, schema, permissions, rejection semantics, or business rules.
- Do not combine this card with market policy extraction.
- Preserve testability with explicit clock injection where currently supported.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.ApplicationTests --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Public contract changes.
- Pricing or quote-lifecycle refactors.
- Moving unrelated services into new configuration classes.
- Global architecture migration.

## Suggested Commit Message

`refactor(craftalism-api): move market wiring into spring configuration`

## Completion Notes

- Added focused `MarketServiceConfiguration` Spring wiring and grouped scalar values in `MarketSettings`.
- Reduced `MarketService` to facade delegation while preserving its transactional public methods.
- Preserved independent quote and execute `MarketTradeRequestPolicy` instances and fixed-clock testability.
- Validation passed from `java/`: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.ApplicationTests --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest`.
