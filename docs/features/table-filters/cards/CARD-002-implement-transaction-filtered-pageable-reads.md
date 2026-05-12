# CARD-002: Implement Transaction Filtered Pageable Reads

## Status

planned

## Objective

Implement `GET /api/transactions` as a filterable pageable read endpoint for dashboard table filters.

## Context

The dashboard must not filter only the currently loaded page. Transaction filters must be applied by the API before pagination.

Depends on:

- `CARD-001-add-transaction-table-filter-contract.md`

## Required Reading

- `../contract.md`
- `CARD-001-add-transaction-table-filter-contract.md`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionController.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionService.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/TransactionRepository.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/GlobalExceptionHandler.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/SecurityConfig.java`

## Expected Behavior

`GET /api/transactions` accepts optional transaction table filters, applies them before pagination, returns `Page<TransactionResponseDTO>`, and preserves existing detail and sender/receiver lookup behavior.

## Acceptance Criteria

- [ ] The endpoint accepts all transaction filters defined in `../contract.md`.
- [ ] Empty or omitted filters return the unfiltered pageable transaction list.
- [ ] Filters are optional and composable.
- [ ] Filters apply before pagination.
- [ ] Default sorting is `createdAt,DESC`, then `id,DESC`.
- [ ] Explicit sorting supports only documented properties.
- [ ] Invalid filter values return repository-standard validation errors.
- [ ] Existing transaction detail and sender/receiver routes remain available.
- [ ] Tests cover filtered, unfiltered, invalid, empty, pagination, sorting, and security behavior.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionController.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/TransactionRepository.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/security/SecurityFilterChainTest.java
```

## Constraints

- Do not implement dashboard behavior.
- Do not mutate transaction, balance, transfer, or player state from read endpoints.
- Do not weaken write-route authorization.
- Do not remove existing transaction lookup routes.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionControllerTest --tests io.github.HenriqueMichelini.craftalism.api.service.TransactionServiceTest --tests io.github.HenriqueMichelini.craftalism.api.security.SecurityFilterChainTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Dashboard implementation
- Market trade filters
- Write-side transaction behavior
- Authentication rollout

## Completion Notes

