# CARD-024: Extract Scheduler Selection Policy

## Status

completed

## Objective

Extract deterministic market-event eligibility, cooldown, and weighted-selection logic from the scheduled infrastructure component into a focused policy collaborator.

## Context

Run this card after the market package reorganization cards. `MarketEventScheduler` currently combines Spring scheduling, transaction setup, distributed lease acquisition, event-window timing, eligibility and cooldown filtering, weighted template selection, and event construction.

This extraction isolates the already-testable selection rules while leaving scheduling and lease mechanics in the infrastructure component.

## Required Reading

- `../contract.md`
- `../../market-pressure-ladder/contract.md`

## Expected Behavior

Scheduled event windows, lease acquisition, eligibility, cooldown rules, weighted selection, event construction, persistence, and transaction behavior remain unchanged while eligibility and selection rules live in a focused policy collaborator.

## Acceptance Criteria

- [ ] Eligibility filtering, cooldown checks, same-target checks, and weighted template selection are extracted from `MarketEventScheduler`.
- [ ] The extracted collaborator receives explicit inputs and does not own Spring scheduling or transaction setup.
- [ ] Lease acquisition, event-window timing, event construction, and lifecycle start behavior remain in `MarketEventScheduler`.
- [ ] Existing random-driven behavior remains deterministic under existing test inputs.
- [ ] No scheduling configuration, persistence, schema, permission, or transaction-boundary behavior changes.
- [ ] Focused scheduler tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/scheduling/MarketEventScheduler.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/domain/event/MarketEventSelectionPolicy.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/
```

## Constraints

- Do not change scheduler probabilities, cooldown behavior, target matching, random call order, lease behavior, or event construction.
- Do not introduce interfaces, abstract classes, or a strategy framework.
- Do not change public contracts.
- Do not mix template catalog extraction into this card.

## Validation Commands

```bash
rtk ./gradlew test --tests '*MarketEventSchedulerTest' --tests '*MarketEventSchedulerSpringTransactionTest'
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Scheduler feature changes.
- New event selection rules.
- Lease redesign.
- Event-template catalog extraction.

## Suggested Commit Message

`refactor(market-events): extract scheduler selection policy`

## Completion Notes

- Extracted deterministic eligibility, cooldown, same-target, sorting, and
  weighted-selection logic into `MarketEventSelectionPolicy`.
- Kept scheduling, transaction setup, lease acquisition, event windows,
  construction, lifecycle start, and random call ordering in the scheduler.
- Verified with focused scheduler tests and the full Gradle test task.
