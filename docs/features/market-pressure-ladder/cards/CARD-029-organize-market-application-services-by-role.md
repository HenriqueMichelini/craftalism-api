# CARD-029: Organize Market Application Services by Role

## Status

completed

## Objective

Mechanically split market application services into facade, command, query, and admin packages.

## Context

Run this card after `CARD-028`. The coarse market package contains controller-facing services and package-private orchestration collaborators with distinct application roles.

The target packages are:

- `market.application`
- `market.application.command`
- `market.application.query`
- `market.application.admin`

## Required Reading

- `../contract.md`
- `../../market-events/contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Market APIs and orchestration remain unchanged while application services are grouped by facade, command, query, and admin responsibilities.

## Acceptance Criteria

- [ ] `MarketService` is moved to `market.application`.
- [ ] Quote, execute, trade execution, and player resolution collaborators are moved to `market.application.command`.
- [ ] Snapshot, state-read, trade-history read, and public event-context collaborators are moved to `market.application.query`.
- [ ] Dashboard and event administration services are moved to `market.application.admin`.
- [ ] Imports and matching tests are updated.
- [ ] Visibility changes are limited to the smallest changes required for cross-package collaboration.
- [ ] No behavior, DTO, endpoint, repository, schema, permission, or transaction-boundary behavior changes.
- [ ] Full project tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketQuoteService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketExecuteService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketTradeExecutor.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketPlayerResolver.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketSnapshotService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketReadService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketTradeHistoryReadService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventPublicContextService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/DashboardMarketCategoryService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/DashboardMarketItemService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketDriftAdminService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventAdminService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/MarketEventTemplateService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/MarketCatalogInitializer.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
```

## Constraints

- Do not rename classes.
- Do not move domain or infrastructure collaborators in this card.
- Do not redesign orchestration or Spring wiring.
- Do not change public contracts or test expectations.
- Do not introduce interfaces or abstract classes.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Domain collaborator package moves.
- Infrastructure collaborator package moves.
- Behavior extraction.
- Naming cleanup.

## Suggested Commit Message

`refactor(market): organize application services by role`

## Completion Notes

- Moved market application services into facade, command, query, and admin role packages.
- Updated imports, matching tests, controller/config wiring, and only the visibility required by the new package boundaries.
- Verified with `rtk ./gradlew test` from `java/`.
