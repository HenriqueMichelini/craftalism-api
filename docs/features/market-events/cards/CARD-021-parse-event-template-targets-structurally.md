# CARD-021: Parse Event Template Targets Structurally

## Status

completed

## Objective

Replace scheduler substring parsing of event-template target metadata with a narrow structured JSON codec.

## Context

`MarketEventScheduler.firstMetadataValue(...)` extracts `categoryIds` and `itemIds` from persisted JSON using string markers and indexes. Authored template metadata is JSON, so target selection and cooldown matching currently depend on formatting details rather than JSON structure.

This is a focused maintainability refactor. Persisted metadata shape and scheduler behavior must remain stable.

## Required Reading

- `../contract.md`
- `CARD-003-add-named-market-event-persistence-and-templates.md`
- `CARD-006-implement-market-event-scheduler-with-guardrails.md`

## Expected Behavior

Scheduler target selection and cooldown matching continue to use the first configured category or item target while valid template metadata is decoded structurally and malformed or missing target metadata is handled safely.

## Acceptance Criteria

- [ ] `MarketEventScheduler` no longer parses JSON using marker strings, `indexOf`, or substring extraction.
- [ ] A package-private codec decodes the first configured `categoryIds` or `itemIds` value from template metadata.
- [ ] Existing authored template JSON remains unchanged.
- [ ] Missing, empty, or malformed target metadata is handled safely without leaking JSON parsing internals.
- [ ] Tests cover category metadata, item metadata, whitespace-formatted JSON, missing keys, and malformed JSON.
- [ ] Existing scheduler selection and cooldown tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventScheduler.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventTemplateTargetMetadataCodec.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventTemplateTargetMetadataCodecTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventSchedulerTest.java
```

## Constraints

- Do not change the persisted JSON shape.
- Do not change event target selection rules.
- Do not change public API DTOs or database schema.
- Keep the codec limited to template target metadata needed by the scheduler.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTargetMetadataCodecTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerTest
```

Run from `java/`.

## Out of Scope

- Changing admin `selectedItemIds` format.
- Replacing event audit metadata storage.
- Adding event-template CRUD APIs.
- Redesigning event targeting.

## Suggested Commit Message

`refactor(craftalism-api): parse event template targets structurally`

## Completion Notes

- Added a narrow package-private Jackson codec for decoding the first configured
  category or item target from template metadata.
- Replaced scheduler substring parsing with structural decoding while preserving
  first-target selection and the completed bounded cooldown-history read.
- Added codec coverage for category metadata, item metadata, whitespace-formatted
  JSON, missing keys, empty values, missing metadata, and malformed JSON.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTargetMetadataCodecTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerTest`
  passed from `java/`.
