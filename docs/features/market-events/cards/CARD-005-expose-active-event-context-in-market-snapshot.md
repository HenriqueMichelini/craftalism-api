# CARD-005: Expose Active Event Context In Market Snapshot

## Status

planned

## Objective

Expose fuzzy active event context alongside the public market snapshot.

## Context

Players should almost always see enough event information to feel the market is fair, but not exact math. The existing colored item variation remains the main mechanical signal.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Market snapshots include active event context with event name, narrative description, rough temporal state, and broad affected scope. Blocked items are explicit. Exact multipliers, exact target lists for mixed rare events, exact countdowns, and internal source/audit fields are not exposed.

## Acceptance Criteria

- [ ] Snapshot response includes active event context when a named event is active.
- [ ] Event context includes player-facing name, description, category/world flavor if allowed, broad scope hint, and rough temporal label.
- [ ] Snapshot response omits event context when no named event is active.
- [ ] Blocked items remain clearly marked through existing item-level blocked semantics.
- [ ] Public response does not expose exact effect values, exact scheduler rolls, seed data, admin audit metadata, or exact countdown.
- [ ] Public response does not expose rarity labels unless a later product decision explicitly allows them.
- [ ] Existing snapshot price fields remain authoritative.

## Expected Files to Change

```text
docs/features/market-events/contract.md
docs/market-contract-mvp.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not change client code in this repository.
- Do not change item/category ordering for MVP.
- Do not expose inactive event history.
- Do not expose exact formulas.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest
```

Run from `java/`.

## Out of Scope

- Dashboard/admin event APIs.
- Public event archive.
- UI rendering.

## Completion Notes
