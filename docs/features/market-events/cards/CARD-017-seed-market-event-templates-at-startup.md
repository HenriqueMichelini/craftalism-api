# CARD-017: Seed Market Event Templates At Startup

## Status

completed

## Objective

Seed the initial market event templates during API startup so runtime databases can create and schedule named market events.

## Context

Runtime investigation on 2026-05-31 found an empty `market_event_templates` table. `MarketEventTemplateService.seedInitialTemplatesIfEmpty()` defines an idempotent seed operation, but no production startup path invokes it. Existing isolated tests verify template definitions without proving runtime initialization.

## Required Reading

- `../contract.md`

## Expected Behavior

API startup invokes the existing idempotent template seed operation. An empty runtime database receives the initial authored templates, while an already-seeded database is left unchanged.

## Acceptance Criteria

- [ ] API startup invokes `MarketEventTemplateService.seedInitialTemplatesIfEmpty()`.
- [ ] An empty template repository receives the initial authored templates.
- [ ] An already-populated template repository is not overwritten or duplicated.
- [ ] A focused startup or initializer test proves the runtime wiring.
- [ ] Existing template-definition tests continue to pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/config/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventTemplateTest.java
```

## Constraints

- Do not change authored template values.
- Do not change scheduler selection, event lifecycle, pricing, blocking, or admin API behavior.
- Do not add a migration that inserts mutable authored templates.
- Preserve idempotent startup behavior.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.config.MarketCatalogInitializerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTest
```

Run from `java/`.

## Out of Scope

- Invalid-template HTTP error mapping.
- Dashboard template discovery or selector UI.
- New market event templates.

## Completion Notes

- Wired the existing idempotent market-event template seed operation into the API startup runner.
- Added focused startup wiring and repository idempotency coverage.
- Validated with the card's targeted Gradle test command.
