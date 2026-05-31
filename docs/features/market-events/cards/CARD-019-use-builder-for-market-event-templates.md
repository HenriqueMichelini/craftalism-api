# CARD-019: Use Builder For Market Event Templates

## Status

completed

## Objective

Replace positional market event template seed construction with a readable builder without changing authored template values or seed behavior.

## Context

`MarketEventTemplateService` constructs each authored template through a positional helper with 17 arguments. The adjacent market catalog seed code uses a package-private builder to keep authored values readable and avoid positional argument mistakes.

This card is a behavior-preserving template seed refactor.

## Required Reading

- `../contract.md`

## Expected Behavior

Initial market event templates retain their existing IDs, metadata, durations, effects, flags, cooldowns, and timestamps while construction uses named builder methods.

## Acceptance Criteria

- [ ] `MarketEventTemplateService` constructs initial templates with named builder methods instead of a positional helper.
- [ ] The builder creates `MarketEventTemplate` entities with all existing authored fields.
- [ ] Initial authored template values remain unchanged.
- [ ] Empty and populated repository seed behavior remains unchanged.
- [ ] Existing focused template tests pass.

## Expected Files to Change

```text
docs/features/market-events/cards/CARD-019-use-builder-for-market-event-templates.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventTemplateService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventTemplateBuilder.java
```

## Constraints

- Do not change authored template values.
- Do not change persistence mappings or public API contracts.
- Do not change scheduler selection, event lifecycle, pricing, blocking, or admin API behavior.
- Keep the builder package-private and limited to market event template seed construction.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTest
```

Run from `java/`.

## Out of Scope

- New market event templates.
- Template validation changes.
- Persistence changes.
- Public API changes.

## Suggested Commit Message

`refactor(craftalism-api): build market event templates with named fields`

## Completion Notes

- Added package-private `MarketEventTemplateBuilder` with named methods for every seeded entity field.
- Replaced the 17-argument template helper with builder-based authored template construction.
- Preserved existing authored template values and idempotent repository seed behavior.
- Ran `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTest` from `java/`: passed.
