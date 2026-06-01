# CARD-023: Extract Default Event Template Catalog

## Status

completed

## Objective

Extract built-in market event template definitions from the event-template application service into a focused default template catalog.

## Context

Run this card after the market package reorganization cards. `MarketEventTemplateService` currently owns startup seeding, dashboard list/create use cases, DTO validation, entity mapping, and the built-in default template list.

The built-in template definitions are cohesive bootstrap catalog data and can be extracted without changing template behavior.

## Required Reading

- `../contract.md`
- `../../market-pressure-ladder/contract.md`

## Expected Behavior

Template seeding, dashboard template APIs, validation, persistence, event scheduling, and public market behavior remain unchanged while built-in template definitions live in a focused catalog collaborator.

## Acceptance Criteria

- [ ] Built-in template definitions are extracted from `MarketEventTemplateService`.
- [ ] A focused default event-template catalog collaborator supplies the same templates with the same values and ordering.
- [ ] `MarketEventTemplateService.seedInitialTemplatesIfEmpty()` preserves existing persistence behavior.
- [ ] Dashboard template list/create behavior and validation remain unchanged.
- [ ] No DTO, endpoint, repository, schema, permission, scheduling, or transaction-boundary behavior changes.
- [ ] Focused event-template tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/domain/event/DefaultMarketEventTemplateCatalog.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventTemplateApiIntegrationTest.java
```

## Constraints

- Preserve every seeded template field and ordering.
- Do not redesign template persistence or validation.
- Do not add interfaces, abstract classes, or a generic catalog framework.
- Do not change scheduler behavior.

## Validation Commands

```bash
rtk ./gradlew test --tests '*MarketEventTemplateTest' --tests '*DashboardMarketEventTemplateApiIntegrationTest' --tests '*MarketCatalogInitializerTest'
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- New templates.
- Template schema changes.
- Scheduler policy extraction.
- Dashboard API changes.

## Suggested Commit Message

`refactor(market-events): extract default template catalog`

## Completion Notes

- Added `DefaultMarketEventTemplateCatalog` as the focused collaborator for the
  four built-in event template definitions.
- Updated `MarketEventTemplateService` to persist catalog templates while
  preserving the existing empty-repository guard and `saveAll` behavior.
- Extended `MarketEventTemplateTest` to verify every authored template field,
  timestamp, and ordering.
- Ran
  `rtk ./gradlew test --tests '*MarketEventTemplateTest' --tests '*DashboardMarketEventTemplateApiIntegrationTest' --tests '*MarketCatalogInitializerTest'`
  from `java/`: passed.
- Ran `rtk ./gradlew test` from `java/`: passed.
