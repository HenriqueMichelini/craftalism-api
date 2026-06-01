# CARD-028: Establish Coarse Market Package Boundary

## Status

completed

## Objective

Mechanically move market-specific classes from the generic service package into a coarse market package.

## Context

The service-layer package audit confirmed that market classes dominate `api.service` and hide application, domain, and infrastructure roles. Twenty-two package-private service-layer types and same-package tests make an immediate leaf-package split unnecessarily risky.

This card establishes `io.github.HenriqueMichelini.craftalism.api.market` as an intermediate boundary. Later cards split that coarse package by role.

## Required Reading

- `../contract.md`
- `../../market-events/contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

All market behavior, APIs, persistence, pricing, quote lifecycle, execution, events, scheduling, bootstrap behavior, and Spring wiring remain unchanged while market-specific classes are located under `io.github.HenriqueMichelini.craftalism.api.market`.

## Acceptance Criteria

- [ ] Every market-specific class currently under `api.service` is moved to `api.market`.
- [ ] Non-market services and table-filter helpers are not moved by this card.
- [ ] Package declarations and imports are updated mechanically.
- [ ] Same-package market service tests are moved to the matching coarse market test package.
- [ ] Existing package-private visibility is preserved.
- [ ] No runtime behavior, DTO, endpoint, repository, schema, permission, or transaction-boundary behavior changes.
- [ ] Full project tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/*Market*.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DashboardMarket*.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DefaultMarketCatalog.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/MarketCatalogInitializer.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/*Market*.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/*Market*.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/config/MarketCatalogInitializerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/*Market*.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/exceptions/MarketExceptionHandlerContractTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/security/SecurityFilterChainTest.java
```

## Constraints

- Do not split market classes into leaf role packages yet.
- Do not rename classes.
- Do not change visibility.
- Do not move table-filter helpers.
- Do not change market behavior or public contracts.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Leaf market application, domain, or infrastructure package splits.
- Market behavior extraction.
- Catalog initializer renaming.
- `MarketReadService` renaming.

## Suggested Commit Message

`refactor(market): establish coarse market package boundary`

## Completion Notes

- Moved market-specific service classes and matching tests into the coarse
  `api.market` package without changing behavior.
- Verified with the full Gradle test task before the leaf-package split.
