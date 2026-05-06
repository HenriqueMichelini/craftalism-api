# CARD-002: Map Pressure Fields On MarketItem

## Status

planned

## Objective

Update `MarketItem` to represent pressure-ladder authoritative state.

## Context

Source: audit finding 1.

`craftalism-api` owns the market aggregate root.

## Required Reading

- `../contract.md`

## Expected Behavior

`MarketItem` exposes the pressure-ladder configuration and state fields required by the database schema.

## Acceptance Criteria

- [ ] `MarketItem` exposes all required pressure config and state fields.
- [ ] Legacy segment relationship remains only if still needed for migration compatibility.
- [ ] No behavior changes are made outside model mapping.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketItem.java
```

## Constraints

- Do not rewrite planner.
- Do not change public DTOs.
- Keep JPA mapping compatible with existing migrations and tests.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/`. Prefer focused compile/model/repository tests when available.

## Out of Scope

- Planner changes.
- Public DTO changes.

## Suggested Commit Message

`feat(craftalism-api): map pressure fields on market items`

## Completion Notes

