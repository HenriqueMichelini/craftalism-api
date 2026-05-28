# CARD-012: Add Active Event Lifecycle And Locking

## Status

planned

## Objective

Persist named event lifecycle state with database-backed enforcement of the MVP one-active-named-event invariant.

## Context

The Market Events MVP allows at most one active named event globally. Service-only checks are not sufficient when concurrent requests or multiple application instances can start, supersede, or expire events.

## Required Reading

- `../contract.md`

## Expected Behavior

Named event instances have wall-clock start/end timestamps and persisted lifecycle status. An event affects pricing or availability only when `status == ACTIVE`, `startedAt <= now`, and `endsAt > now`. Persistence and service logic prevent two active named events from existing at the same time.

## Acceptance Criteria

- [ ] Event instance persistence includes lifecycle status, `startedAt`, `endsAt`, and end reason.
- [ ] Effective active-event queries use status plus wall-clock start/end timestamps.
- [ ] Expired events stop affecting snapshots and quotes even if no cleanup transition has run.
- [ ] Snapshot, quote, scheduler, or admin paths can opportunistically transition expired events to `EXPIRED`.
- [ ] A database-backed guard prevents more than one active named event globally.
- [ ] Tests cover concurrent or repeated attempts to create two active events.

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

- Do not apply event price modifiers in this card.
- Do not add scheduler selection in this card.
- Do not add public event history.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketEventMigrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventLifecycleServiceTest
```

Run from `java/`.

## Out of Scope

- Price effects.
- Drift state.
- Scheduler windows.
- Admin HTTP APIs.

## Completion Notes
