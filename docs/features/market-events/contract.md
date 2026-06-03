# Market Events Contract

## Purpose

Define repo-local backend ownership, stable rules, and validation boundaries for Market Events.

Market Events add shared world conditions that can temporarily affect market prices or market access while preserving the pressure-ladder market model.

## Source of Truth

- `../../market-pressure-ladder-sigmoid-pricing.md` remains the authoritative pricing model.
- `../../market-contract-mvp.md` remains the public market API contract.
- `../market-pressure-ladder/contract.md` remains the pressure-ladder feature contract.

Market Events must extend those rules. They must not simplify, reinterpret, or bypass pressure-ladder pricing, sell percentage pricing, stale quote semantics, min/max clamps, blocked semantics, or operating semantics.

## Related Documents

- `handoff.md` is a non-authoritative implementation handoff for follow-on work.

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
- Drift uses persistence and timestamps separate from pressure regeneration state. It must not reuse `lastUpdatedAt`, because `lastUpdatedAt` controls pressure regeneration.
- Drift may update lazily or through a timed process, but it must still be evaluated for balanced items where `netPosition == 0`. MVP tuning should target roughly 0.5 to 1 hour between drift evaluations.
- Drift movement should be designer-tunable. The initial MVP band should be small and stock-like, around -6% to +6% per drift evaluation, with a tighter cumulative cap or mean-reversion rule so drift cannot run away.
- Drift must not mutate `netPosition`.
- Drift applies to every tradable item, though later item profiles may tune sensitivity.
- Drift may continue while named events are active because it is tiny and separate.
- Drift applies through the shared pricing pipeline after pressure price derivation and before named event modifiers and final min/max clamp.
- Snapshot estimates, quote totals, execute prices, and `variationPercent` must reflect drift whenever drift affects current price.

## Pricing Pipeline Rules

Market Events require one shared pricing pipeline used by snapshot projection, quote planning, and quote-backed execution.

The MVP pricing order is:

1. derive pressure buy price from the pressure ladder
2. apply per-item drift
3. apply the active named event modifier, if one applies
4. clamp the adjusted buy price within `minUnitPrice` and `maxUnitPrice`
5. derive sell price from the adjusted buy price using `sellPricePercentage`

Implementations must not add drift or named event modifiers in only the snapshot path, only the quote path, or only the execute path.

SELL prices must continue to derive from the adjusted buy price using the configured `sellPricePercentage`; event behavior must not introduce a separate sell-side formula for MVP.

## Named Event Rules

- Named events start at medium rarity. Very common market movement is represented by drift.
- The MVP supports only one active named event at a time.
- Named events are globally shared world state.
- Named events are systemically random or admin-triggered, never player-targeted.
- Named event price effects apply as temporary modifiers in the shared pricing pipeline after pressure-plus-drift pricing and before final min/max clamp.
- Named event price effects must not bypass `minUnitPrice` or `maxUnitPrice`.
- Named event effects stop affecting new snapshots and new quotes immediately when the event ends.
- Prices return to normal pressure-plus-drift behavior immediately after an event ends.
- Named event duration is wall-clock based. An event is price-effective only when its lifecycle status is active and the current time is within its start/end timestamps.
- Expired events may be transitioned to an expired status opportunistically by snapshot, quote, scheduler, or admin paths, but pricing must not depend on cleanup having already run.
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

Event instances must persist selected targets explicitly enough for audit. Category-scoped events store the selected category target and resolve currently affected items dynamically during pricing. Item-scoped events store explicit item targets. Rare mixed-target events should store explicit item targets to avoid ambiguous pricing.

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
- whether mixed effects are allowed
- whether blocking is allowed
- cooldown behavior
- player-facing name
- player-facing narrative description
- player-facing broad scope hint

Templates should be category-flavored where possible. Generic templates are allowed, but common player-facing events should feel like world conditions rather than raw math.

Templates may repeat, but rare and extra-rare templates need enough variety and cooldowns that repeats feel natural rather than spammy.

Templates must not create neutral no-effect events.

## Dashboard Template Admin API

Template administration is an internal dashboard/admin API owned by
`craftalism-api`.

Template routes require `SCOPE_market:admin`. Generic `SCOPE_api:write` is not
sufficient.

Persisted template rows are exposed through:

- `GET /api/dashboard/market/event-templates`
- `POST /api/dashboard/market/event-templates`
- `PUT /api/dashboard/market/event-templates/{templateId}`

Template create requests include:

- `templateId`
- `rarity`
- `scope`
- `automaticWeight`
- `automaticEnabled`
- `blockingAllowed`
- `minDurationSeconds`
- `maxDurationSeconds`
- `minEffectBasisPoints`
- `maxEffectBasisPoints`
- `cooldownSeconds`
- `playerFacingName`
- `playerFacingDescription`
- `broadScopeHint`
- `eligibleTargetMetadata`

Template update requests use the same authored fields except `templateId`:

- `rarity`
- `scope`
- `automaticWeight`
- `automaticEnabled`
- `blockingAllowed`
- `minDurationSeconds`
- `maxDurationSeconds`
- `minEffectBasisPoints`
- `maxEffectBasisPoints`
- `cooldownSeconds`
- `playerFacingName`
- `playerFacingDescription`
- `broadScopeHint`
- `eligibleTargetMetadata`

For updates, `templateId` is path-bound and immutable. The update request body
must not define template identity, and the API does not perform template rename
or upsert behavior. Updating an unknown `templateId` returns a validation-style
client error and does not create a template.

`effectDirection` is derived by `craftalism-api` from the authored effect
basis-point range. Ranges entirely above `10000` derive `UP`; ranges entirely
below `10000` derive `DOWN`; ranges with both bounds exactly `10000` derive
`BLOCK` and remain subject to blocking-template validation. Mixed ranges that
cross or include `10000` without being exactly neutral are invalid. Neutral
`10000` ranges are valid only for manual rare or extra-rare item templates that
allow blocking.

Template create and update responses return the persisted template row:

- `templateId`
- `rarity`
- `scope`
- `automaticWeight`
- `automaticEnabled`
- `blockingAllowed`
- `minDurationSeconds`
- `maxDurationSeconds`
- `minEffectBasisPoints`
- `maxEffectBasisPoints`
- `effectDirection`
- `cooldownSeconds`
- `playerFacingName`
- `playerFacingDescription`
- `broadScopeHint`
- `eligibleTargetMetadata`
- `createdAt`
- `updatedAt`

`craftalism-api` owns template validation, normalization, persistence,
timestamps, scheduler semantics, pricing semantics, and lifecycle semantics.
Dashboard/front-end repositories may submit authored create or update requests
and render returned template rows; they must not calculate authoritative
template validation, persistence, scheduler behavior, pricing behavior, or
lifecycle semantics locally.

Template delete behavior is out of scope unless a later card explicitly adds it.

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

Market Events split snapshot freshness from quote execution validity.

`snapshotVersion` remains the browse and quote-creation freshness token. Once the backend issues a quote, execute must validate the quote token, quote expiry, single-use lifecycle, request identity, effective item availability, and the stored quote price promise. Execute must not reject solely because the current market snapshot version changed after quote creation.

Quotes preserve the pricing conditions they were created under until quote expiry.

Quote persistence must store an immutable price promise and lightweight pricing context metadata sufficient for audit and debugging. Context metadata may include the pricing context version, drift value or revision, event instance id, event effect version, and base pressure position used at quote creation.

Execute should settle using the stored quote unit and total price after validity checks. It must not recompute a current event-adjusted price and reject only because drift moved or a price event ended after quote creation.

If a named price event ends after quote creation, the quote price remains valid until expiry.

If an item becomes effectively blocked after quote creation and before execution, execute must reject.

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

Event blocking is derived effective availability, not a mutation of `MarketItem.blocked`.

Effective blocked state is:

```text
effectiveBlocked = item.blocked || activeBlockingEventTargetsItem
```

Snapshots expose effective blocked state. Quote and execute reject when effective blocked state is true. Event blocking must not overwrite, restore, or otherwise mutate durable item blocked state.

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

- uses a scheduled worker with a database lock or lease so only one application instance rolls an event window at a time
- uses jittered event windows
- may choose to start nothing
- does not roll a meaningful named event on every market update
- uses named-event windows that are rarer than drift evaluations and preserve stretches of normal market behavior
- does not start automatic events while the market is globally closed; MVP uses the existing config-backed market closure state exposed as a dependency
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
- The MVP enforces one active named event globally in both service logic and persistence.
- Drift can always exist.
- Named event modifiers do not stack in MVP. Drift always applies, and at most one named event may apply.
- Manual supersede has priority over the current active named event. Supersede ends the previous event immediately with reason `SUPERSEDED`, then starts the replacement event.
- New named events must not target the same item/category/market immediately after a related event ends.
- If an admin action supersedes an active event, the previous event ends immediately with reason `SUPERSEDED`.

## Lifecycle Rules

Named event instances use wall-clock start and end timestamps plus persisted lifecycle status.

An event is effective only when:

```text
status == ACTIVE
startedAt <= now
endsAt > now
```

The persistence model must defend the MVP one-active-named-event invariant with a database-backed guard, not only an in-memory check.

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

Admin mutation controls must not rely only on generic `SCOPE_api:write`. They require a narrower event-admin authority such as `SCOPE_market:admin`, or an explicitly documented equivalent dashboard/admin authority if the wider platform defines one.

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
