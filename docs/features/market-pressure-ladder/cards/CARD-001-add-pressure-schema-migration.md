# CARD-001: Add Pressure Schema Migration

## Status

planned

## Objective

Add pressure-ladder columns to `market_items` and deterministic legacy backfill from `market_segments`.

## Context

Source: audit findings 1, 2, and 3.

`craftalism-api` owns durable market state and Flyway migrations.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Pressure-ladder state exists on `market_items` with a deterministic legacy backfill from segment state.

## Acceptance Criteria

- [ ] `market_items` has `base_unit_price`, `min_unit_price`, `max_unit_price`, `segment_size`, `price_sensitivity`, `base_regen_quantity`, `regen_interval_seconds`, `net_position`, `min_net_position`, and `max_net_position`.
- [ ] `net_position` is backfilled as `sum(max_capacity - remaining_capacity)`.
- [ ] Backfill checks `current_stock == sum(remaining_capacity)` and fails or clearly flags inconsistent legacy state.

## Expected Files to Change

```text
java/src/main/resources/db/migration/V15__*.sql
java/src/test/...
```

## Constraints

- Do not switch runtime code to pressure planning.
- Do not remove `market_segments`.
- Keep migration deterministic and auditable.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Also run any Flyway or migration-specific test available.

## Out of Scope

- Runtime pressure planning.
- Legacy segment removal.

## Suggested Commit Message

`feat(craftalism-api): add pressure market state migration`

## Completion Notes

