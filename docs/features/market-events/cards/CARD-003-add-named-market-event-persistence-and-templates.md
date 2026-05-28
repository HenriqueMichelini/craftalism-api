# CARD-003: Add Named Market Event Persistence And Templates

## Status

planned

## Objective

Persist named market events and hand-authored event templates without applying them to prices yet.

## Context

Named events are authored world conditions. Templates carry rarity, weight, scope, duration range, effect range, blocking capability, cooldown behavior, and player-facing narrative text.

## Required Reading

- `../contract.md`

## Expected Behavior

The backend can store event templates and active/ended event instances with enough internal data for audit, telemetry, replay, and admin visibility. No event should affect market prices until a later card wires pricing.

## Acceptance Criteria

- [ ] Add persistence for event templates or seedable template definitions.
- [ ] Add persistence for event instances with source, rarity, scope, selected targets, effect roll, duration, start/end timestamps, status, end reason, and audit metadata.
- [ ] Support item, category, item-set, and market-wide scopes conceptually.
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
