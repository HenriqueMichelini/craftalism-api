# Market Events Contract

## Purpose

Define repo-local backend ownership, stable rules, and validation boundaries for Market Events.

Market Events add shared world conditions that can temporarily affect market prices or market access while preserving the pressure-ladder market model.

## Source of Truth

- `../../market-pressure-ladder-sigmoid-pricing.md` remains the authoritative pricing model.
- `../../market-contract-mvp.md` remains the public market API contract.
- `../market-pressure-ladder/contract.md` remains the pressure-ladder feature contract.

Market Events must extend those rules. They must not simplify, reinterpret, or bypass pressure-ladder pricing, sell percentage pricing, stale quote semantics, min/max clamps, blocked semantics, or operating semantics.

## Repository Ownership

`craftalism-api` owns authoritative backend market event state, event templates, event scheduling, event audit data, event pricing modifiers, item blocking effects, quote interaction, snapshot event context, and admin controls.

`craftalism-market` consumes market snapshots, active event context, quotes, and execute responses. It must not calculate authoritative event effects locally.

## Goals

- Make the market feel dynamic without turning normal market behavior into a slot machine.
- Add globally shared market conditions that create social reaction and strategic adaptation.
- Support small ambient movement through unnamed drift.
- Support larger temporary market shifts through named events.
- Preserve trustworthy displayed prices and quote-backed execution.
- Keep luck present without making luck the only meaningful strategy.

## Non-Goals

- Do not implement client behavior in this repository.
- Do not create player-targeted events.
- Do not mutate player-owned balances, inventory, or assets as an event effect.
- Do not add public event history for MVP.
- Do not expose exact event formulas, exact multipliers, scheduler rolls, or random seeds to players.
- Do not implement seasons, event chains, direct trade-reactive event generation, or automatic full-market shutdowns for MVP.
- Do not enable automatic extra-rare events unless a later card explicitly scopes a conservative feature flag.

## Core Concepts

Market Events have two separate concepts:

- unnamed market drift
- named market events

Drift is ambient per-item movement. It is always small, tightly capped, and separate from player-driven pressure.

Named events are explicit temporary world conditions. They use authored templates, have a rarity, duration, scope, narrative description, and one or more effects.

## Drift Rules

- Drift is unnamed and must not be presented as an event.
- Drift is per item for MVP.
- Drift affects actual trade prices, not display-only decoration.
- Drift must be small, bounded, and mean-reverting or otherwise constrained so prices stay readable.
- Drift should update on the same lazy or timed market update rhythm, but less visibly than named events. MVP tuning should target roughly 0.5 to 1 hour between drift evaluations.
- Drift movement should be designer-tunable. The initial MVP band should be small and stock-like, around -6% to +6% per drift evaluation, with a tighter cumulative cap or mean-reversion rule so drift cannot run away.
- Drift must not mutate `netPosition`.
- Drift applies to every tradable item, though later item profiles may tune sensitivity.
- Drift may continue while named events are active because it is tiny and separate.
- Drift applies after pressure price derivation and before final min/max clamp.
- Snapshot estimates, quote totals, execute prices, and `variationPercent` must reflect drift whenever drift affects current price.

## Named Event Rules

- Named events start at medium rarity. Very common market movement is represented by drift.
- The MVP supports only one active named event at a time.
- Named events are globally shared world state.
- Named events are systemically random or admin-triggered, never player-targeted.
- Named event price effects apply as temporary modifiers to current pressure-plus-drift prices before final min/max clamp.
- Named event price effects must not bypass `minUnitPrice` or `maxUnitPrice`.
- Named event effects stop affecting new snapshots and new quotes immediately when the event ends.
- Prices return to normal pressure-plus-drift behavior immediately after an event ends.
- Named event duration is wall-clock based. Lazy or timed market updates must observe expired events and stop applying them immediately to new snapshots and new quotes.
- Event source is stored internally but is not normally player-facing.

## Rarity Rules

Supported conceptual rarities:

- `MEDIUM`
- `RARE`
- `EXTRA_RARE`

Very common movement is drift, not a named event rarity.

Rarity influences:

- scheduler probability and template weight
- target scope eligibility
- effect strength
- duration range
- cooldown requirements
- whether blocking is allowed
- whether automatic selection is allowed

Pain should scale by rarity, but harsh effects should target market access or profitability rather than player-owned assets.

Strength and duration should usually have an inverse relationship. Stronger events should usually be shorter; milder events may last longer.

## Scope Rules

Supported conceptual scopes:

- item
- item set
- category
- market-wide

MVP implementation should focus primarily on item, category, and market-wide scopes. Item-set scope may exist conceptually or internally but should not be emphasized until needed.

High-value items may be eligible for events, but should have lower weights and stronger cooldowns.

Every item should eventually have an event profile for eligibility, weights, cooldown class, sensitivity, and allowed event types. MVP cards may implement profiles incrementally.

## Template Rules

Templates are hand-authored first.

Templates define:

- rarity
- automatic selection weight
- allowed source
- scope
- eligible targets
- duration range
- effect range
- effect direction
- whether mixed effects are allowed
- whether blocking is allowed
- cooldown behavior
- player-facing name
- player-facing narrative description
- player-facing broad scope hint

Templates should be category-flavored where possible. Generic templates are allowed, but common player-facing events should feel like world conditions rather than raw math.

Templates may repeat, but rare and extra-rare templates need enough variety and cooldowns that repeats feel natural rather than spammy.

Templates must not create neutral no-effect events.

## Player Visibility Rules

Players should usually see enough active event context before committing to a trade to feel that the market is fair.

Public market experience should expose:

- active event name
- narrative description
- rough temporal language
- broad affected scope hint
- clear blocked item state when relevant

Public market experience should not expose:

- exact multipliers
- exact target lists for mixed rare+ events
- exact countdowns
- public rarity labels unless a later product decision explicitly allows them
- internal random seed or roll data
- scheduler decision data
- admin audit metadata
- whether an event was admin-triggered unless a later product decision changes this

Existing item price variation remains the main mechanical signal.

## Quote And Execute Rules

Quotes preserve the event conditions they were created under until quote expiry.

If a named price event ends after quote creation, the quote price remains valid until expiry.

If an item becomes blocked after quote creation and before execution, execute must reject.

Outstanding quotes are not proactively cancelled by blocking events.

Event behavior must preserve:

- single-use quote lifecycle
- stale quote semantics
- quote expiration semantics
- failed settlement no-mutation behavior
- pressure mutation direction
- sell percentage pricing

## Blocking Rules

Blocking is allowed in MVP only for rare/manual events.

Automatic blocking, if enabled, must be narrow and safe:

- item-level only
- short duration
- strong cooldown
- low weight
- safer items first

For MVP, a blocking event should usually only block. It should not bundle price effects by default.

Full market shutdown must not be automatic for MVP.

## Scheduler Rules

Automatic named events use weighted randomness with guardrails.

Scheduler behavior:

- uses jittered event windows
- may choose to start nothing
- does not roll a meaningful named event on every market update
- uses named-event windows that are rarer than drift evaluations and preserve stretches of normal market behavior
- does not start automatic events while the market is globally closed
- uses template weights and eligibility
- uses cooldowns per item, category, market, and template
- stores generated decisions and exact rolls internally for audit and telemetry
- may use market state to adjust weights

State-aware weighting should not overprotect players. Double-lucky and double-unlucky outcomes are allowed sometimes. State should adjust likelihood, not hard-block outcomes, except for conflicts, cooldowns, market closure, or explicit eligibility rules.

Automatic rare events are allowed only for less dangerous rare templates at first.

Automatic extra-rare events are disabled for MVP unless a later card explicitly scopes a conservative feature flag.

## Conflict Rules

- An item can have at most one named event modifier at a time.
- A category can have at most one category event at a time.
- A market-wide event blocks other named events while active.
- The MVP should enforce one active named event globally.
- Drift can always exist.
- New named events must not target the same item/category/market immediately after a related event ends.
- If an admin action supersedes an active event, the previous event ends immediately with reason `SUPERSEDED`.

## Admin Rules

Admin controls are required.

Admins may:

- inspect active and recent/internal event state
- trigger events manually
- cancel active events
- supersede active events
- edit event state only through explicitly audited paths

Admin actions may bypass selected scheduler guardrails, but must be warning-backed and auditable.

Admin event source and exact values are internal metadata.

## Audit And Telemetry Rules

The backend should store maximum useful internal event data, then filter what is exposed to players.

Internal event records should support:

- exact effect values
- exact duration values
- source
- target selection
- random seed or roll metadata where applicable
- scheduler decision metadata
- start and end timestamps
- end reason
- admin actor if available
- cancellation or supersession reason

## External Interfaces

Market Events may affect:

- `GET /api/market/snapshot`
- `POST /api/market/quotes`
- `POST /api/market/execute`
- market snapshot item DTOs
- market snapshot active event context
- market rejection codes for blocked/non-tradable items
- admin/dashboard event APIs
- Flyway migrations for durable state changes

## Cross-Feature Dependencies

Market Events depend on the pressure-ladder market feature and must preserve its invariants.

Balance settlement and transfer behavior remain authoritative in this repository and must not be weakened by event behavior.

`craftalism-market` is an out-of-repo consumer and must not be changed here.

## Public Contract Change Rules

Changes to APIs, DTOs, schemas, persistence, rejection semantics, permissions, security, or external behavior require explicit card scope.

## Persistence Rules

Market event persistence must be deterministic, auditable, and migration-safe.

Event records should preserve internal values even when public responses intentionally expose only fuzzy event information.

## Security And Permission Rules

Do not weaken authentication, authorization, idempotency, transfer safety, quote safety, incident handling, or admin/dashboard access control while implementing Market Events.

Admin event controls must use existing admin/dashboard security patterns.

## Validation Rules

Use the validation command listed by the selected card.

Prefer focused service and controller tests for narrow cards. Use `rtk ./gradlew test` from `java/` when the selected card changes cross-service market behavior.

## Source Areas

- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/`
- `java/src/main/resources/db/migration/`

## Test Areas

- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/`
