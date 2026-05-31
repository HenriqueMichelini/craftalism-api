# CARD-026: Consolidate Market Trade Request Policy

## Status

completed

## Objective

Extract the duplicated quote and execute request checks into one focused package-private market trade request policy.

## Context

`MarketQuoteService` and `MarketExecuteService` independently implement market-open rejection, quantity validation, effective blocked-state rejection, operating-state rejection, rate-limit rejection, and market rejection construction. The market-events contract requires quote and execute availability behavior to remain aligned.

This is a behavior-preserving duplication refactor. Quote creation and execute lifecycle orchestration remain separate.

## Required Reading

- `../contract.md`
- `../../market-events/contract.md`
- `CARD-019-extract-market-quote-orchestration.md`
- `CARD-020-extract-market-execute-orchestration.md`

## Expected Behavior

Quote and execute requests retain their existing rejection codes, messages, HTTP statuses, snapshot-version behavior, rate limiting, and availability checks while shared request rules are implemented once.

## Acceptance Criteria

- [ ] A package-private market trade request policy owns shared market-open, positive-quantity, effective-blocked, operating-state, and rate-limit checks.
- [ ] `MarketQuoteService` delegates shared checks to the policy and retains quote-specific orchestration.
- [ ] `MarketExecuteService` delegates shared checks to the policy and retains quote-consumption and settlement orchestration.
- [ ] Quote and execute rejection codes, messages, HTTP statuses, and snapshot-version values remain unchanged.
- [ ] Event-derived effective blocking remains applied in both quote and execute paths.
- [ ] Existing quote, execute, and market contract tests pass without expectation changes.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeRequestPolicy.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketQuoteService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketExecuteService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketQuoteServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
```

## Constraints

- Do not change quote lifecycle, quote TTL, token format, or single-use semantics.
- Do not change settlement, pressure mutation, or pricing rules.
- Do not change public API DTOs, rejection contracts, or endpoint behavior.
- Do not introduce a generic validation framework.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

## Out of Scope

- Extracting quote lifecycle policy.
- Extracting execute settlement behavior.
- Changing market rate-limit configuration.
- Moving market dependency wiring into Spring configuration.

## Suggested Commit Message

`refactor(craftalism-api): consolidate market trade request policy`

## Completion Notes

- Added package-private `MarketTradeRequestPolicy` for shared market-open,
  positive-quantity, effective-blocked, operating-state, rate-limit, and
  rejection-construction behavior.
- Updated quote and execute orchestration to delegate shared request checks
  while retaining quote creation, quote consumption, and settlement flow.
- Preserved independent quote and execute rate limiters by wiring one policy
  instance per request path in `MarketService`.
- Validation passed from `java/`:
  `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest`.
