# CARD-004: Prepare Shared Table Filter Contract Evidence

## Status

planned

## Objective

Prepare repo-local API contract evidence for shared contract documentation consumed by `craftalism-dashboard`.

## Context

The dashboard `CARD-001` remains blocked until shared/API contracts explicitly define filter query semantics for Transactions and Market Trades. This repository can provide implemented API evidence and update repo-local public API docs, but shared-root contract changes must be made in the owning shared-contract repository.

Depends on:

- `CARD-002-implement-transaction-filtered-pageable-reads.md`
- `CARD-003-extend-market-trade-dashboard-filters.md`

## Required Reading

- `../contract.md`
- `CARD-002-implement-transaction-filtered-pageable-reads.md`
- `CARD-003-extend-market-trade-dashboard-filters.md`
- repo-local public API contract docs

## Expected Behavior

Repo-local contracts document the final implemented filter semantics clearly enough to support a shared-contract follow-up outside this repository.

## Acceptance Criteria

- [ ] Repo-local public API docs define filter query semantics for `GET /api/transactions`.
- [ ] Repo-local public API docs define filter query semantics for `GET /api/market/trades`.
- [ ] Repo-local public API docs state that filters apply before pagination.
- [ ] Repo-local public API docs define text match modes, numeric ranges, instant ranges, enum casing, empty result shape, and invalid value behavior.
- [ ] A separate shared-contract follow-up is identified for the owning shared-contract repository.

## Expected Files to Change

```text
docs/features/table-filters/contract.md
docs/market-contract-mvp.md
```

## Constraints

- Do not implement production code in this card.
- Do not define dashboard-owned behavior in API docs.
- Do not modify root shared contracts from this repository.
- Stop at the repository boundary and hand off shared-root contract changes to the owning repository.

## Validation Commands

```bash
git diff --check
```

## Out of Scope

- Dashboard implementation
- Backend implementation
- Auth rollout
- Shared-root contract implementation
- Sorting beyond documented table sort parameters

## Completion Notes
