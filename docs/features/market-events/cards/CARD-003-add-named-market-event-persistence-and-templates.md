# CARD-003: Add Named Market Event Persistence And Templates

## Status

completed

## Objective

Persist named market events and hand-authored event templates without applying them to prices yet.

## Context

Named events are authored world conditions. Templates carry rarity, weight, scope, duration range, effect range, blocking capability, cooldown behavior, and player-facing narrative text.

This card persists template and instance data only. Runtime lifecycle enforcement and database-backed one-active-event locking are handled by `CARD-012`.

## Required Reading

- `../contract.md`
- `CARD-012-add-active-event-lifecycle-and-locking.md`

## Expected Behavior

The backend can store event templates and event instances with enough internal data for audit, telemetry, replay, and admin visibility. No event should affect market prices or item availability until later cards wire lifecycle, pricing, and blocking behavior.

## Acceptance Criteria

- [ ] Add persistence for event templates or seedable template definitions.
- [ ] Add persistence for event instances with source, rarity, scope, selected targets, effect roll, duration, start/end timestamps, status, end reason, and audit metadata.
- [ ] Support item, category, item-set, and market-wide scopes conceptually.
- [ ] Store category-scoped event targets as selected category ids.
- [ ] Store item-scoped and mixed rare event targets as explicit item ids.
- [ ] Store exact internal values while preserving later ability to expose only fuzzy player-facing details.
- [ ] Seed initial medium and safe rare templates.
- [ ] Extra-rare templates are manual-only or disabled for automatic selection.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/resources/db/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not apply event modifiers to pricing in this card.
- Do not make event instances effective for pricing or blocking in this card.
- Do not expose public player APIs yet.
- Do not create player-targeted events.
- Do not implement scheduler selection yet.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketEventMigrationTest
```

Run from `java/`.

## Out of Scope

- Price effects.
- Quote effects.
- Scheduler windows.
- Admin mutation APIs.

## Completion Notes

- Added named market event template and instance persistence models, repositories, and V22 schema.
- Stored source, rarity, scope, selected targets, effect roll/version, duration window, lifecycle status, end reason, actor, and audit metadata for event instances.
- Added conceptual support for item, item-set, category, and market-wide scopes; category targets store category ids, and item/item-set targets store explicit item ids.
- Added initial medium automatic templates, a safe rare manual blocking template, and manual-only/automatic-disabled extra-rare template coverage.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketEventMigrationTest` passed from `java/`.
- Reverification fix: changed the manual-only extra-rare seed from a neutral no-effect template to an explicit non-neutral price-effect template while keeping automatic selection disabled.
- Reverification validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateTest --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketEventMigrationTest` passed from `java/`.
