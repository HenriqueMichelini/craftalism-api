# CARD-003: Add Pressure Catalog Defaults And Validation

## Status

planned

## Objective

Replace segment-count catalog configuration with pressure-pricing defaults and hard validation.

## Context

Source: audit finding 10.

`craftalism-api` owns backend market item configuration.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Catalog seed data defines pressure-ladder defaults and rejects invalid pressure configuration.

## Acceptance Criteria

- [ ] Catalog items define `baseUnitPrice`, `minUnitPrice`, `maxUnitPrice`, `segmentSize`, `priceSensitivity`, `baseRegenQuantity`, `regenIntervalSeconds`, and optional bounds.
- [ ] Validation enforces all hard rules from the design doc.
- [ ] New seed items initialize `netPosition = 0`.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DefaultMarketCatalog.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItem.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializer.java
java/src/test/...
```

## Constraints

- Do not remove legacy segment migration code.
- Do not change quote or execute behavior yet.
- Use documented initial defaults unless explicit overrides are required.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Prefer catalog initializer unit tests when available.

## Out of Scope

- Quote behavior changes.
- Execute behavior changes.
- Legacy segment migration removal.

## Suggested Commit Message

`feat(craftalism-api): seed pressure market catalog defaults`

## Completion Notes

Implemented in:

- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DefaultMarketCatalog.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItem.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializer.java`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializerTest.java`

Validation:

- `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketCatalogInitializerTest`
- `rtk ./gradlew test`
