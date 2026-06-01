# CARD-033: Rename Market Read Service for Lazy Refresh

## Status

completed

## Objective

Rename `MarketReadService` to `MarketSnapshotStateLoader` so its lazy persisted refresh behavior is explicit.

## Context

Run this card after `CARD-031`. The service-layer package audit confirmed that `MarketReadService.regeneratedItems()` does more than a side-effect-free read: it locks eligible items, applies pressure regeneration and drift evaluation, saves changed entities, and returns refreshed state for snapshot projection.

The behavior is contract-compatible and must remain unchanged. The current name is misleading.

## Required Reading

- `../contract.md`
- `../../market-events/contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Snapshot-backed lazy pressure regeneration and drift refresh behave exactly as before while the collaborator name communicates that snapshot state loading can persist refreshes.

## Acceptance Criteria

- [ ] `MarketReadService` is renamed to `MarketSnapshotStateLoader`.
- [ ] The nested read-state type is renamed only if needed for naming consistency.
- [ ] Imports, construction sites, and matching tests use the new name.
- [ ] No lazy refresh, locking, regeneration, drift, persistence, timing, snapshot, or transaction-boundary behavior changes.
- [ ] Focused snapshot/read tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/query/MarketReadService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/query/MarketSnapshotStateLoader.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/query/MarketSnapshotService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/configuration/MarketServiceConfiguration.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
```

## Constraints

- Rename only.
- Do not alter lazy mutation behavior.
- Do not extract regeneration or drift logic.
- Do not change public contracts or transaction boundaries.

## Validation Commands

```bash
rtk ./gradlew test --tests '*MarketReadServiceTest' --tests '*MarketQuoteServiceTest' --tests '*MarketServiceTest'
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Behavior extraction.
- Regeneration redesign.
- Drift tuning.
- Snapshot contract changes.

## Suggested Commit Message

`refactor(market): clarify snapshot state loader naming`

## Completion Notes

- Renamed `MarketReadService` to `MarketSnapshotStateLoader` and the nested
  state type to `MarketSnapshotState`.
- Verified with corrected focused selectors and a serial full Gradle test run.
