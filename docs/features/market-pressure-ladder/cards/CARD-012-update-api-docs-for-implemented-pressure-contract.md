# CARD-012: Update API Docs For Implemented Pressure Contract

## Status

completed

## Objective

Align repo-local public docs after implementation matches the pressure-ladder contract.

## Context

Source: audit findings 7, 8, 9, and 11.

`craftalism-api` owns canonical backend market API docs. `craftalism-market` consumes documented API behavior.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../../craftalism-market-pressure-ladder-changelog.md`
- `../../../../README.md`

## Expected Behavior

Repo-local public docs describe the implemented pressure contract and no longer present superseded target stock semantics.

## Acceptance Criteria

- [ ] Docs show pressure fields, not target `currentStock`.
- [ ] `INSUFFICIENT_STOCK` docs mention only configured hard pressure bounds.
- [ ] Snapshot version docs match authoritative pressure hash semantics.

## Expected Files to Change

```text
README.md
docs/market-contract-mvp.md
docs/craftalism-market-pressure-ladder-changelog.md
```

Only change repo-local market contract docs if drift remains after implementation.

## Constraints

- Do not document behavior before it is implemented.
- Do not edit client repository docs.
- Docs should trail code behavior unless a card explicitly changes only docs.

## Validation Commands

```bash
rtk ./gradlew test
```

Run from `java/` when contract tests are part of the implementation. Also perform manual doc review.

## Out of Scope

- Client repository docs.
- Pre-implementation contract claims.

## Suggested Commit Message

`docs(craftalism-api): align market docs with pressure contract`

## Completion Notes

- Updated repo-local market docs to describe implemented pressure snapshot fields, hard pressure-bound `INSUFFICIENT_STOCK` semantics, and authoritative pressure-state `snapshotVersion` hash behavior.
- Validation: `rtk ./gradlew test` from `java/` passed.
