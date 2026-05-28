# CARD-001: Document Market Events Contract

## Status

completed

## Objective

Create the authoritative backend contract for Market Events.

## Context

Market Events need a repo-local source of truth before implementation because the feature changes pricing, quotes, blocking, scheduler behavior, admin behavior, and public snapshot context.

## Required Reading

- `../../../market-pressure-ladder-sigmoid-pricing.md`
- `../../../market-contract-mvp.md`
- `../../market-pressure-ladder/contract.md`

## Expected Behavior

A new feature contract defines Market Events as backend-owned behavior and records the approved MVP rules: unnamed per-item drift, named event templates, one active named event, weighted scheduler, admin override, quote preservation, blocking semantics, and player-facing event visibility.

## Acceptance Criteria

- [x] Add `docs/features/market-events/contract.md`.
- [x] Contract states `craftalism-api` owns authoritative event state, pricing modifiers, scheduler, admin controls, quote interaction, and snapshot event context.
- [x] Contract preserves pressure-ladder pricing, sell percentage pricing, min/max clamps, stale quote behavior, and blocked/operating semantics.
- [x] Contract separates unnamed drift from named events.
- [x] Contract defines MVP out-of-scope items: public history, seasons, exact countdowns, automatic extra-rare events, player-targeted events, and automatic full market shutdown.

## Expected Files to Change

```text
docs/features/market-events/contract.md
docs/features/index.md
```

## Constraints

- Do not change source code.
- Do not redefine `craftalism-market` behavior beyond identifying it as a consumer.
- Do not weaken the existing market pressure-ladder contract.

## Validation Commands

```bash
rg "market-events|Market Events" docs/features docs/index.md
```

## Out of Scope

- Persistence.
- Runtime behavior.
- API implementation.
- UI changes.

## Completion Notes

- Added `docs/features/market-events/contract.md` as the authoritative repo-local Market Events feature contract.
- Updated `docs/features/index.md` so the feature is routed from the feature index.
- Validation: `rg "market-events|Market Events" docs/features docs/index.md` passed.
