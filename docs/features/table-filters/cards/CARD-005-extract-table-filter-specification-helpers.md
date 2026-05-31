# CARD-005: Extract Table Filter Specification Helpers

## Status

completed

## Objective

Extract shared table-filter validation and JPA specification primitives used by transaction and market trade-history reads.

## Context

`TransactionService` and `MarketTradeHistoryReadService` independently implement UUID exact and contains matching, UUID parsing, inclusive numeric and instant range validation, allowed-sort validation, and default-sort pageable handling. Both services implement the same table-filter API contract.

This card consolidates stable primitives while leaving resource-specific filters and query assembly in their owning services.

## Required Reading

- `../contract.md`

## Expected Behavior

Transaction and market trade-history list reads retain their current filtering, validation, pagination, sorting, and response behavior while shared filter primitives are implemented once.

## Acceptance Criteria

- [ ] Package-private table-filter helpers provide UUID exact and case-insensitive contains specifications.
- [ ] Package-private table-filter helpers provide inclusive numeric-range, instant-range, allowed-sort, and default-sort pageable validation or construction where behavior is shared.
- [ ] `TransactionService` retains transaction-specific filter composition and transaction mutation behavior.
- [ ] `MarketTradeHistoryReadService` retains market-trade-specific filter composition and DTO mapping.
- [ ] Existing validation messages and HTTP-facing behavior remain unchanged.
- [ ] Existing transaction and market-trade-history service and controller tests pass without expectation changes.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TableFilterSpecifications.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TableFilterValidation.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/TransactionServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketControllerTest.java
```

## Constraints

- Do not change filter query parameters, match modes, response shapes, or sorting rules.
- Do not introduce a generic filtering framework or reflection-based query builder.
- Do not move resource-specific predicates into shared helpers.
- Do not change public API contracts.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.TransactionServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeHistoryReadServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest
```

Run from `java/`.

## Out of Scope

- New table filters.
- Strict rejection of unsupported query parameters.
- Shared-root contract publication.
- Dashboard changes.

## Suggested Commit Message

`refactor(craftalism-api): extract table filter specification helpers`

## Completion Notes

- Extracted package-private `TableFilterSpecifications` and
  `TableFilterValidation` helpers for shared UUID matching, inclusive bounds,
  range validation, sort validation, and default-sort pageable construction.
- Kept transaction-specific composition and mutations in `TransactionService`.
- Kept market-trade-specific composition and DTO mapping in
  `MarketTradeHistoryReadService`.
- Verified with the declared focused Gradle test command.
