# CARD-020: Bound Scheduler Cooldown History Reads

## Status

completed

## Objective

Replace per-template full event-history reads during scheduler cooldown evaluation with one bounded recent-event lookup per scheduler roll.

## Context

`MarketEventScheduler.eligibleTemplates(...)` evaluates cooldowns once per candidate template. The current `isCoolingDown(...)` implementation calls `eventRepository.findAll()` for each template, so scheduler cost grows with both template count and accumulated event history.

This is a behavior-preserving scalability refactor. The scheduler must retain the existing template, target, rarity, lease, and cooldown semantics.

## Required Reading

- `../contract.md`
- `CARD-006-implement-market-event-scheduler-with-guardrails.md`

## Expected Behavior

Scheduler decisions remain unchanged while each eligible-template evaluation pass loads only the recent event history required by candidate cooldown windows and performs at most one bounded event-history repository query.

## Acceptance Criteria

- [ ] `MarketEventScheduler` no longer calls `eventRepository.findAll()` from per-template cooldown evaluation.
- [ ] Scheduler cooldown evaluation loads recent events once per roll using a repository query bounded by the earliest relevant cooldown cutoff.
- [ ] Template, category, item, item-set, and market-wide cooldown comparisons remain unchanged.
- [ ] Scheduler behavior remains unchanged when no templates are eligible.
- [ ] Focused tests verify that multiple candidate templates do not cause repeated event-history repository reads.
- [ ] Existing scheduler lease, jitter, rarity, and transaction-boundary tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventScheduler.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketEventInstanceRepository.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventSchedulerTest.java
```

## Constraints

- Do not change scheduler probabilities, jitter, lease behavior, or event selection rules.
- Do not change cooldown duration values or target matching semantics.
- Do not change persistence schema or public API contracts.
- Do not introduce caching across scheduler rolls.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerSpringTransactionTest
```

Run from `java/`.

## Out of Scope

- Structured parsing of template target metadata.
- Scheduler policy redesign.
- New indexes or database migrations.
- New event templates.

## Suggested Commit Message

`refactor(craftalism-api): bound scheduler cooldown history reads`

## Completion Notes

- Added a repository lookup bounded by event creation timestamp.
- Scheduler cooldown evaluation now loads recent event history once per eligible-template pass using the longest candidate cooldown as the earliest relevant cutoff.
- Preserved per-template cooldown filtering and existing template/target matching semantics.
- Added focused coverage proving that multiple candidate templates result in one bounded cooldown-history read and no full-history repository read.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerSpringTransactionTest` passed from `java/`.
