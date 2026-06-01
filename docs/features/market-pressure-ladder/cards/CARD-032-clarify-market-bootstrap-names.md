# CARD-032: Clarify Market Bootstrap Names

## Status

completed

## Objective

Rename the two market catalog initializer classes so startup orchestration and persisted catalog reconciliation are unambiguous.

## Context

Run this card after `CARD-031`. The service-layer package audit confirmed two distinct classes named `MarketCatalogInitializer`:

- an `ApplicationRunner` that invokes startup initialization
- a collaborator that seeds and reconciles persisted catalog values

The shared name obscures navigation and review context.

## Required Reading

- `../contract.md`

## Expected Behavior

Market startup and catalog reconciliation behavior remain unchanged while the application runner is named `MarketStartupInitializer` and the reconciliation collaborator is named `MarketCatalogBootstrapper`.

## Acceptance Criteria

- [ ] The bootstrap `ApplicationRunner` is renamed to `MarketStartupInitializer`.
- [ ] The persisted catalog reconciliation collaborator is renamed to `MarketCatalogBootstrapper`.
- [ ] Imports, construction sites, and matching tests use the new names.
- [ ] No startup ordering, catalog values, persistence behavior, Spring component behavior, or transaction-boundary behavior changes.
- [ ] Focused bootstrap tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/bootstrap/MarketCatalogInitializer.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/bootstrap/MarketStartupInitializer.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/bootstrap/MarketCatalogBootstrapper.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/configuration/MarketServiceConfiguration.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
```

## Constraints

- Rename classes only.
- Do not change startup orchestration or catalog reconciliation logic.
- Do not change public contracts.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test --tests '*MarketCatalogInitializerTest' --tests '*MarketServiceTest'
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Catalog behavior changes.
- Template seed extraction.
- Spring wiring redesign.
- Scheduler changes.

## Suggested Commit Message

`refactor(market): clarify catalog bootstrap names`

## Completion Notes

- Renamed the startup runner to `MarketStartupInitializer` and the persisted
  catalog reconciler to `MarketCatalogBootstrapper`.
- Verified with corrected focused selectors and the full Gradle test task.
