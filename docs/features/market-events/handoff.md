# Market Events Handoff

## Purpose

This handoff summarizes the implemented Market Events feature for follow-on backend, dashboard, and client work.

Authoritative behavior remains in `contract.md`. Implementation detail and validation evidence remain in the cards under `cards/`.

## Ownership Boundary

`craftalism-api` owns authoritative market event state, event templates, event scheduling, event audit data, event pricing modifiers, derived blocking, quote interaction, snapshot event context, and dashboard/admin event controls.

`craftalism-market` and other clients consume backend snapshots, quotes, execute responses, and public active event context. Clients must not calculate authoritative drift, event effects, blocked state, quote validity, or event lifecycle locally.

## Feature State

Market Events are implemented in the backend feature cards.

Completed cards:

| Card | Delivered Behavior |
|---|---|
| `CARD-001` | Authoritative Market Events contract and feature routing. |
| `CARD-002` | Durable per-item drift state, bounded drift evaluation, snapshot-version integration, and quote pricing context drift capture. |
| `CARD-003` | Named event template and instance persistence, initial seeded templates, and audit-capable event metadata. |
| `CARD-004` | Active named event price modifiers in the shared pricing path for snapshots and quote planning, with execute settling from stored quote prices. |
| `CARD-005` | Optional public `activeEvent` snapshot context with fuzzy player-facing information only. |
| `CARD-006` | Scheduled automatic named event worker with durable lease, jittered windows, cooldowns, market-closure guard, and rarity guardrails. |
| `CARD-007` | Dashboard/admin event APIs for list, manual start, update, cancel, and supersede. |
| `CARD-008` | Rare/manual item-level blocking events as derived effective availability. |
| `CARD-009` | Quote execute validity split from browse snapshot freshness. |
| `CARD-010` | Immutable quote pricing context metadata for pressure, drift, and event audit/debugging. |
| `CARD-011` | Shared market pricing pipeline across snapshot projection, quote planning, and quote-backed execution. |
| `CARD-012` | Active named event lifecycle, wall-clock effectiveness, opportunistic expiration, and database-backed one-active-event guard. |
| `CARD-013` | Dedicated `SCOPE_market:admin` boundary for event-admin routes. |

## Runtime Semantics

Market pricing now flows through one shared pipeline:

1. derive pressure buy price from the pressure ladder
2. apply per-item drift
3. apply the active named event modifier when eligible
4. clamp within item min/max prices
5. derive sell price from the adjusted buy price using `sellPricePercentage`

Snapshots, quote planning, and quote-backed execution must continue using this shared path. New market pricing work should extend the pipeline instead of adding side-path calculations.

Quotes are durable price promises. Quote creation still requires a fresh browse `snapshotVersion`, but execute no longer rejects only because the current market snapshot changed after quote creation. Execute validates the stored quote identity, status, expiry, single-use lifecycle, request fields, item existence, operating state, and effective blocked state, then settles using stored quote unit and total prices.

Named event price effects end for new snapshots and quotes as soon as the event is no longer effectively active. Already-issued quotes keep their stored price until expiry unless the item becomes effectively blocked or another quote validity rule fails.

## Public API Surface

Public market snapshots may include optional `activeEvent` context when a named event is effectively active. Public context is intentionally fuzzy: player-facing name, description, broad scope hint, and rough temporal label.

Public responses must not expose exact effect values, exact target lists for mixed rare events, exact scheduler rolls, seed data, source, admin actor, audit metadata, exact countdowns, or public rarity labels.

Item `blocked` semantics now represent effective blocked state in snapshots. Effective blocked means durable item blocking or active event blocking. Event blocking must remain derived from event state and must not mutate `MarketItem.blocked`.

## Admin Surface

Dashboard/admin event routes live under:

```text
/api/dashboard/market/events
```

Admin reads and mutations expose internal event metadata only to callers with the dedicated event-admin authority. Generic `SCOPE_api:write` is not sufficient for these routes.

Implemented admin capabilities:

- list active and recent/internal event instances
- manually start events
- update editable event state through audited paths
- cancel active events
- supersede active events, ending the previous event with `SUPERSEDED`

Public `/api/market/**` routes must remain free of admin metadata and must not reveal whether an event was admin-triggered.

## Scheduler Notes

The automatic scheduler is a timed worker, not lazy read-time generation. It uses a durable lease so only one application instance rolls an event window at a time.

Scheduler behavior includes:

- skip when disabled
- skip while the market is closed
- skip when another named event is effectively active
- jittered windows that are rarer than drift evaluations
- possible no-event outcomes
- cooldown checks for item, category, market, and template repeats
- automatic extra-rare events disabled by default
- blocking rare templates excluded from automatic selection
- stored roll, duration, effect, source, and decision metadata

## Follow-On Guidance

Backend changes that affect pricing, quote execution, blocking, lifecycle, scheduler behavior, admin permissions, public DTOs, or persistence still require an explicit card because those are public or durable behavior boundaries.

Client/dashboard follow-on work should consume backend-provided state:

- display public `activeEvent` context when present
- treat `snapshotVersion` and quote tokens as opaque
- continue using quote totals as authoritative
- display backend-provided item blocked state as effective availability
- avoid showing exact event math, internal rarity, source, or admin metadata
- avoid recalculating drift or named event effects locally

## Validation Evidence

Each card records its targeted validation command and completion result. The combined card evidence covers market planner, snapshot projector, quote service, trade executor, controller integration, migration, lifecycle, scheduler, admin service, and admin security tests.

Recent card-level validations include:

- `MarketTradePlannerTest`
- `MarketSnapshotProjectorTest`
- `MarketTradeExecutorTest`
- `MarketQuoteServiceTest`
- `MarketContractIntegrationTest`
- `MarketEventMigrationTest`
- `MarketEventTemplateTest`
- `MarketEventLifecycleServiceTest`
- `MarketEventSchedulerTest`
- `DashboardMarketEventAdminApiIntegrationTest`
- `DashboardMarketEventAdminSecurityTest`

## Known Constraints

The MVP still excludes public event history, seasons, event chains, player-targeted events, direct trade-reactive event generation, automatic full-market shutdowns, public exact countdowns, and automatic extra-rare events unless a later card explicitly scopes them.

One active named event globally remains an MVP invariant. Drift can coexist with a named event, but named event modifiers do not stack.
