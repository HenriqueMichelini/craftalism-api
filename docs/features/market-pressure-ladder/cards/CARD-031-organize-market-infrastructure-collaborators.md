# CARD-031: Organize Market Infrastructure Collaborators

## Status

completed

## Objective

Mechanically move market bootstrap, codec, configuration, scheduling, and quote-store collaborators into infrastructure packages.

## Context

Run this card after `CARD-030`. The remaining coarse market classes are infrastructure concerns rather than application services or domain rules.

The existing config-level `MarketCatalogInitializer` application runner is also part of market bootstrap and should move without being renamed. Naming cleanup is handled separately.

## Required Reading

- `../contract.md`
- `../../market-events/contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Market startup, catalog reconciliation, target metadata parsing, Spring wiring, scheduled event rolls, and quote persistence transitions behave exactly as before while infrastructure classes are grouped by responsibility.

## Acceptance Criteria

- [ ] Catalog reconciliation and the startup application runner are moved to `market.infrastructure.bootstrap`.
- [ ] Target metadata codec is moved to `market.infrastructure.codec`.
- [ ] Market Spring wiring is moved to `market.infrastructure.configuration`.
- [ ] Event scheduler is moved to `market.infrastructure.scheduling`.
- [ ] Quote store is moved to `market.infrastructure.store`.
- [ ] Imports and matching tests are updated.
- [ ] Visibility changes are limited to the smallest changes required for cross-package collaboration.
- [ ] No behavior, DTO, endpoint, repository, schema, permission, scheduling, bootstrap, or transaction-boundary behavior changes.
- [ ] Full project tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketCatalogInitializer.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/MarketCatalogInitializer.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventTemplateTargetMetadataCodec.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketServiceConfiguration.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventScheduler.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketQuoteStore.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/config/MarketCatalogInitializerTest.java
```

## Constraints

- Do not rename either `MarketCatalogInitializer` class in this card.
- Do not change scheduling configuration, quote lifecycle semantics, bootstrap behavior, or Spring bean wiring.
- Do not rewrite target metadata parsing.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Naming cleanup.
- Behavior extraction.
- Configuration model redesign.
- Scheduler policy changes.

## Suggested Commit Message

`refactor(market): organize infrastructure collaborators`

## Completion Notes

- Moved bootstrap, codec, configuration, scheduling, and quote-store
  collaborators into infrastructure packages.
- Used `bootstrap.runner` for the startup runner until the duplicate
  initializer names were clarified by `CARD-032`.
- Verified with the full Gradle test task.
