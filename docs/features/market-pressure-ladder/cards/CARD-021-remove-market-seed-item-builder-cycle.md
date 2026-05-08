# CARD-021: Remove MarketSeedItem Builder Cycle

## Status

completed

## Objective

Break the `MarketSeedItem$Builder -> MarketSeedItem` class cycle without changing catalog seed values.

## Context

RefactorFirst identified `MarketSeedItem$Builder -> MarketSeedItem` as a relationship removal priority. The current nested builder and `MarketSeedItem.builder()` factory create a class cycle around a package-private seed record.

This card is a behavior-preserving catalog seed refactor.

## Required Reading

- `../contract.md`

## Expected Behavior

Default market catalog seed values and validation remain unchanged while `MarketSeedItem` no longer owns a nested builder that references the record.

## Acceptance Criteria

- [ ] `MarketSeedItem` no longer declares a nested `Builder`.
- [ ] Catalog seed construction remains readable and validates through the `MarketSeedItem` compact constructor.
- [ ] Existing invalid seed validation tests remain covered.
- [ ] Default seed item values remain unchanged.
- [ ] RefactorFirst no longer reports the `MarketSeedItem$Builder -> MarketSeedItem` cycle after rerunning the report.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItem.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItemBuilder.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DefaultMarketCatalog.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializerTest.java
```

## Constraints

- Do not change seed validation rules.
- Do not change default catalog item IDs, categories, prices, bounds, segment sizes, sensitivity, or regeneration values.
- Do not change catalog initialization behavior.
- Do not change public API DTOs or endpoint contracts.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- MarketService refactoring.
- Persistence changes.
- Public API changes.
- Market segment table cleanup.

## Suggested Commit Message

`refactor(craftalism-api): remove market seed item builder cycle`

## Completion Notes

- Extracted the nested `MarketSeedItem.Builder` into package-private `MarketSeedItemBuilder`.
- Removed `MarketSeedItem.builder()` so `MarketSeedItem` no longer depends on its builder.
- Updated default catalog seed construction and invalid seed validation tests to use `MarketSeedItemBuilder`.
- Ran `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest` from `java/`: passed.
- Ran `rtk ./gradlew test --rerun-tasks --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest` from `java/`: passed.
- RefactorFirst was rerun with the temporary Maven analysis POM and no longer reports the `MarketSeedItem$Builder -> MarketSeedItem` cycle.
