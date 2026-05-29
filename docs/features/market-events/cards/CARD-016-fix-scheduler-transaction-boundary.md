# CARD-016: Fix Scheduler Transaction Boundary

## Status

planned

## Objective

Ensure scheduled market-event lease acquisition runs inside an active transaction.

## Context

Runtime logs show the scheduled market event task failing with `TransactionRequiredException: Executing an update/delete query` when `MarketEventScheduler.acquireLease` calls `MarketEventSchedulerLockRepository.acquireExpiredLease`.

The scheduler currently enters through `scheduledRoll()` and calls `rollWindow()` on the same object. `rollWindow()` is annotated with `@Transactional`, but same-instance invocation bypasses Spring's transactional proxy, so the repository `@Modifying` update can execute without an active transaction.

## Required Reading

- `../contract.md`
- `CARD-006-implement-market-event-scheduler-with-guardrails.md`
- `CARD-012-add-active-event-lifecycle-and-locking.md`

## Expected Behavior

The scheduled market-event worker acquires or skips the scheduler lease without throwing a transaction-required exception. Scheduler behavior, event eligibility, lease semantics, and event-window decisions remain otherwise unchanged.

## Acceptance Criteria

- [ ] The scheduled entry path executes lease acquisition within an active transaction.
- [ ] `MarketEventSchedulerLockRepository.acquireExpiredLease` still performs an atomic lease update and returns the affected row count.
- [ ] Existing scheduler skip/start behavior remains unchanged.
- [ ] Tests cover the scheduled entry path or a Spring-managed invocation path that would fail without the transactional boundary.
- [ ] Existing `MarketEventSchedulerTest` coverage still passes.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventScheduler.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketEventSchedulerLockRepository.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

Repository changes are optional if the service-level transaction boundary is sufficient, but may be used if they keep transaction ownership clearer without changing behavior.

## Constraints

- Do not change scheduler probability, window timing, jitter, rarity filtering, cooldowns, or template eligibility.
- Do not change market event public API behavior.
- Do not change database schema.
- Do not introduce cross-repository behavior.
- Do not implement unrelated scheduler refactors.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerTest
```

Run from `java/`.

If a Spring-managed scheduler transaction test is added in a different test class, run that focused class as well.

## Out of Scope

- Scheduler tuning.
- New market event templates.
- Public event history.
- Admin scheduler controls.
- Database migration changes.

## Completion Notes

