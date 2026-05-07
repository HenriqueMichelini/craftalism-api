# CARD-010: Remove Runtime Segment Dependency

## Status

completed

## Objective

Stop normal market runtime reads, planning, execution, and projections from depending on `market_segments`.

## Context

Source: audit findings 2 and 3.

`craftalism-api` owns normal backend market operation. Legacy segments may remain as migration or audit data only.

## Required Reading

- `../contract.md`

## Expected Behavior

Normal runtime market operations no longer require persisted segment rows.

## Acceptance Criteria

- [ ] Normal snapshot, quote, and execute paths do not fetch `m.segments`.
- [ ] `market_segments` is not required for new catalog runtime behavior.
- [ ] Any remaining segment usage is explicitly migration or audit only.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketItemRepository.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketSegmentRepository.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketSegment.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializer.java
java/src/test/...
```

## Constraints

- Do not drop the `market_segments` table yet unless the pressure path has already been verified.
- Do not alter historical migrations.
- Preserve bounded read behavior.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Include integration and performance tests confirming bounded read behavior without segment fetch fan-out.

## Out of Scope

- Historical migration edits.
- Table drop unless explicitly scoped after pressure path verification.

## Suggested Commit Message

`refactor(craftalism-api): remove runtime market segment dependency`

## Completion Notes

- Removed normal repository fetch joins/entity graphs for `MarketItem.segments`.
- Switched market planning, projection, execution, regeneration, and catalog initialization to derive from pressure-state fields only.
- Default catalog bootstrap no longer creates `market_segments` rows.
- Kept legacy segment entity/repository/table for migration or audit-only access.
- Validated with `rtk ./gradlew test` from `java/`.
