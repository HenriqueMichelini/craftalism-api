# CARD-006: Implement Market Event Scheduler With Guardrails

## Status

planned

## Objective

Implement automatic named event scheduling from eligible templates using weighted randomness with guardrails.

## Context

Automatic events should happen often enough to give the market personality, but not so often that normal market behavior disappears. Event windows are chances, not guarantees.

Named event scheduling is not lazy read-time generation. It uses a scheduled worker guarded by a database lock or lease.

## Required Reading

- `../contract.md`
- `CARD-003-add-named-market-event-persistence-and-templates.md`
- `CARD-012-add-active-event-lifecycle-and-locking.md`

## Expected Behavior

A scheduled worker periodically evaluates whether to start a named event. It uses a database lock or lease so only one application instance rolls an event window at a time. It picks from eligible templates using weights, cooldowns, target eligibility, market state, active-event conflicts, config-backed market closure state, and rarity restrictions.

## Acceptance Criteria

- [ ] Scheduler uses a database lock or lease around event-window decisions.
- [ ] Scheduler uses jittered windows rather than every-update or read-time event rolls.
- [ ] Scheduler may choose to start nothing.
- [ ] Named-event windows are rarer than drift evaluations and preserve stretches of normal market behavior.
- [ ] Only one named automatic event can be active at a time in MVP.
- [ ] No automatic event starts while the config-backed market closure dependency reports the market closed.
- [ ] Cooldowns prevent immediate repeat targeting by item, category, market, and template.
- [ ] State-aware weighting can reduce or increase likelihood but does not hard-block double lucky/unlucky outcomes except for conflicts and cooldowns.
- [ ] Automatic extra-rare events are disabled by default or behind a conservative flag.
- [ ] Automatic rare events include only safe rare templates initially.
- [ ] Event generation stores selected template, random rolls, target, duration, effect, source, and decision metadata.
- [ ] Tests cover duplicate scheduler executions attempting to start events concurrently.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not create player-targeted events.
- Do not implement seasons.
- Do not make direct trade-reactive event triggers.
- Do not start automatic full-market shutdown events.
- Do not generate named events lazily from public snapshot or quote reads.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventSchedulerTest
```

Run from `java/`.

## Out of Scope

- Admin manual trigger APIs.
- Public event history.
- Client notifications.

## Completion Notes
