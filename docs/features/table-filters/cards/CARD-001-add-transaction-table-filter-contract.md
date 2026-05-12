# CARD-001: Add Transaction Table Filter Contract

## Status

completed

## Objective

Define the transaction list request contract for API-backed dashboard table filters.

## Context

`GET /api/transactions` currently returns an unpaged list. Dashboard table filtering needs API-owned query semantics for filtering before pagination.

## Required Reading

- `../contract.md`
- `../../../repo-contract-map.md`
- `../../../context-policy.md`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionController.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionService.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/TransactionRepository.java`

## Expected Behavior

The API has a ready implementation contract for `GET /api/transactions` filters, pagination, sorting, validation, and response shape.

## Acceptance Criteria

- [x] The contract defines `fromPlayerUuid`, `toPlayerUuid`, text match modes, `minAmount`, `maxAmount`, `createdFrom`, and `createdTo`.
- [x] The contract defines filters-before-pagination behavior.
- [x] The contract defines inclusive numeric and instant bounds.
- [x] The contract defines `Page<TransactionResponseDTO>` response expectations.
- [x] The contract defines validation and error behavior for invalid filters.
- [x] The contract documents compatibility expectations for existing unfiltered reads and lookup routes.

## Expected Files to Change

```text
docs/features/table-filters/contract.md
docs/features/table-filters/cards/
```

## Constraints

- Do not implement production code in this card.
- Do not change dashboard behavior.
- Do not remove existing transaction lookup routes.

## Validation Commands

```bash
git diff --check
```

## Out of Scope

- Controller implementation
- Repository query implementation
- Dashboard implementation
- Authentication changes

## Completion Notes

Defined the transaction list filter contract in `docs/features/table-filters/contract.md`, including filter names, match modes, range semantics, pagination, sorting, response shape, invalid value behavior, and compatibility expectations for existing unfiltered and lookup reads.
