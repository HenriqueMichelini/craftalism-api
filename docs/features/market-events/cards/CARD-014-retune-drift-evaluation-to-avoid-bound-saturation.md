# CARD-014: Retune Drift Evaluation To Avoid Bound Saturation

## Status

planned

## Objective

Retune per-item drift evaluation so ordinary elapsed drift ticks do not push most balanced market items to the absolute drift bounds.

## Context

Live local evidence showed 52 of 72 market items at the exact drift bounds after six drift revisions: 26 items at `9400` basis points and 26 items at `10600` basis points. The same database had no `market_event_instances` rows, so the observed saturation is drift-state behavior, not named-event accumulation.

Current drift evaluation starts from the previous drift multiplier, applies a deterministic random step of up to the full absolute bound, applies weak mean reversion, and clamps to `10000 +/- 600`.

## Required Reading

- `../contract.md`

## Expected Behavior

Balanced items continue to receive persistent, bounded, mean-reverting drift, but a small number of normal hourly evaluations must not cause most items to cluster at `-6%` or `+6%`. Drift remains per-item, deterministic for a given item/revision input, and separate from named event pricing.

## Acceptance Criteria

- [ ] Drift evaluation still updates balanced items where `netPosition == 0`.
- [ ] Drift remains bounded by the existing absolute drift cap unless the card explicitly documents and tests a narrower cap.
- [ ] Multi-tick drift regression coverage proves a live-like six-tick evaluation does not place most catalog items at the exact minimum or maximum drift bound.
- [ ] Mean reversion or equivalent constraint is strong enough that drift does not stay pinned at a bound after subsequent ordinary evaluations.
- [ ] Snapshot estimates and `variationPercent` still reflect the drift multiplier after recomputation.
- [ ] Named event pricing behavior, event lifecycle behavior, quote pricing context, and pressure regeneration behavior are unchanged.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketDriftService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketReadServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlannerTest.java
```

## Constraints

- Do not change named event scheduling, event lifecycle, or event pricing semantics.
- Do not change public snapshot DTO fields.
- Do not reset existing production/local drift state in this card.
- Do not change pressure-ladder price derivation.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketReadServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest
```

Run from `java/`.

## Out of Scope

- Admin drift reset or reseed controls.
- Database migration for existing drift state.
- Frontend presentation changes.
- Changing `variationPercent` into separate drift-only and event-adjusted fields.

## Completion Notes

Implemented. Ordinary drift tick movement now uses a narrower deterministic
step while preserving the existing absolute `10000 +/- 600` drift cap. Added
regression coverage for six balanced drift ticks across a catalog-sized item
set and for subsequent ordinary ticks moving pinned drift back inside bounds.
