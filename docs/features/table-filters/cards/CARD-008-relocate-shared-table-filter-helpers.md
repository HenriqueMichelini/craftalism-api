# CARD-008: Relocate Shared Table Filter Helpers

## Status

completed

## Objective

Mechanically move the shared table-filter helpers from the generic service package into `shared.tablefilter`.

## Context

The service-layer package audit confirmed that `TableFilterSpecifications` and `TableFilterValidation` are cross-cutting query utilities rather than application services. They are currently consumed by transaction and market trade-history reads.

This card must run after the transaction service package move so both consumers can import the helpers from their final shared package.

## Required Reading

- `../contract.md`

## Expected Behavior

Transaction and market trade-history filtering, validation, pagination, sorting, response behavior, and HTTP-facing error semantics remain unchanged while the shared helpers are located under `io.github.HenriqueMichelini.craftalism.api.shared.tablefilter`.

## Acceptance Criteria

- [ ] `TableFilterSpecifications` and `TableFilterValidation` are moved to `io.github.HenriqueMichelini.craftalism.api.shared.tablefilter`.
- [ ] Transaction and market trade-history consumers import the moved helpers.
- [ ] Helper visibility is limited to the smallest level required by consumers in separate packages.
- [ ] No filter behavior, validation message, sorting rule, pagination rule, DTO, endpoint, or response shape changes.
- [ ] Focused filter tests and the full project test task pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TableFilterSpecifications.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/TableFilterValidation.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/shared/tablefilter/TableFilterSpecifications.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/shared/tablefilter/TableFilterValidation.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/transaction/application/TransactionService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/transaction/application/TransactionServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketControllerTest.java
```

## Constraints

- Do not redesign helper APIs beyond the smallest visibility changes required by package movement.
- Do not introduce a generic filtering framework or reflection-based query builder.
- Do not move resource-specific predicates into shared helpers.
- Do not change public API contracts or test expectations.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.transaction.application.TransactionServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeHistoryReadServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Moving market trade-history services into market packages.
- Adding filters.
- Changing pageable response behavior.
- Refactoring resource-specific query composition.

## Suggested Commit Message

`refactor(api): relocate shared table filter helpers`

## Completion Notes

- Moved the shared table-filter helpers into `shared.tablefilter` and updated
  transaction and market trade-history consumers.
- Removed the temporary duplicated market match-mode check so both consumers
  use the shared validator.
- Verified with focused filter tests and the final full Gradle test task.
