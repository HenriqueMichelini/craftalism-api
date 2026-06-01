# CARD-030: Organize Market Domain Collaborators by Role

## Status

completed

## Objective

Mechanically split market rule objects, catalog values, event services, snapshot projection, trade planning, and rate limiting into domain-oriented packages.

## Context

Run this card after `CARD-029`. The coarse market package still contains focused rule objects and helpers whose responsibilities are already clear from source and tests.

## Required Reading

- `../contract.md`
- `../../market-events/contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Market pricing, catalog defaults, event effects, snapshot projection, trade planning, and rate limiting behave exactly as before while collaborators are grouped under market domain packages.

## Acceptance Criteria

- [ ] Catalog defaults, seed values, seed builder, and item configuration validation are moved to `market.domain.catalog`.
- [ ] Event lifecycle, blocking, pricing, and template builder classes are moved to `market.domain.event`.
- [ ] Pressure pricing, pricing pipeline, and drift rules are moved to `market.domain.pricing`.
- [ ] Snapshot projection is moved to `market.domain.snapshot`.
- [ ] Trade planning and trade request policy are moved to `market.domain.trade`.
- [ ] Rate limiting is moved to `market.domain.rate`.
- [ ] Imports and matching tests are updated.
- [ ] Visibility changes are limited to the smallest changes required for cross-package collaboration.
- [ ] No behavior, DTO, endpoint, repository, schema, permission, or transaction-boundary behavior changes.
- [ ] Full project tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/DefaultMarketCatalog.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketSeedCategory.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketSeedItem.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketSeedItemBuilder.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketItemConfigurationValidator.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventLifecycleService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventBlockingService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventPricingService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventTemplateBuilder.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketPressurePricing.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketPricingPipeline.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketDriftService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketSnapshotProjector.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketTradePlanner.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketTradeRequestPolicy.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketRateLimiter.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/domain/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
```

## Constraints

- Do not rename classes.
- Do not rewrite pricing, event, snapshot, trade, catalog, or rate-limit behavior.
- Do not move application or infrastructure collaborators in this card.
- Do not introduce interfaces, abstract classes, or new design patterns.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Application or infrastructure package moves.
- Entity redesign.
- Full DDD conversion.
- Behavior extraction.

## Suggested Commit Message

`refactor(market): organize domain collaborators by role`

## Completion Notes

- Moved catalog, event, pricing, snapshot, trade, and rate collaborators into
  their domain-oriented packages with only required visibility changes.
- Verified with `rtk ./gradlew testClasses` and `rtk ./gradlew test`.
