# CARD-015: Reject Post-Consume Plan Mismatch As Stale Quote

## Status

planned

## Objective

Convert post-consume market quote plan mismatches into stable `STALE_QUOTE` market rejections instead of generic server errors.

## Context

Audit evidence confirmed that `MarketService.execute` checks the current snapshot before consuming a quote, then consumes the quote before loading the market item with a pessimistic lock. If market state changes between the pre-check and locked execution, `MarketTradeExecutor.verifyQuotedExecution` can throw `IllegalStateException`, which is handled as a generic internal server error.

The market contract requires quote-backed execution to preserve single-use semantics, stale quote semantics, and stable market rejection responses. The quote may remain consumed after the first execute attempt claims it.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

When a quote is successfully claimed but the locked authoritative item state no longer reproduces the quoted unit price or total price, execute returns the standard market rejection payload with code `STALE_QUOTE`.

The quote remains single-use. Later retries with the same `quoteToken` still reject with `STALE_QUOTE`.

No balance mutation or market pressure mutation occurs for the failed execution.

## Acceptance Criteria

- [ ] A post-consume buy plan mismatch returns `STALE_QUOTE` instead of a generic 500.
- [ ] A post-consume sell plan mismatch returns `STALE_QUOTE` instead of a generic 500.
- [ ] The first mismatched execute attempt leaves the quote consumed or otherwise non-active so retries remain single-use.
- [ ] Balance amount is unchanged after a post-consume stale execution.
- [ ] `netPosition` and derived market projections are unchanged after a post-consume stale execution.
- [ ] Existing `INSUFFICIENT_FUNDS` behavior still consumes the quote and does not mutate market pressure.
- [ ] Existing consumed quote replay behavior still returns `STALE_QUOTE`.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
```

## Constraints

- This card explicitly scopes market execute rejection semantics after a quote has been claimed.
- Do not move quote execution away from single-use semantics.
- Do not retry settlement after a claimed quote fails.
- Do not change pressure-ladder pricing math, quote planning traversal, or snapshot hashing rules.
- Do not change balance settlement behavior except to verify no mutation on stale execution.
- Do not implement client behavior in this repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

Fallback for local iteration:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest
```

## Out of Scope

- Changing quote token format.
- Changing quote expiry behavior.
- Adding rate limiting.
- Changing consumer retry behavior outside this repository.
- Reworking transaction boundaries beyond the smallest stale-rejection fix.

## Completion Notes

Leave empty until implemented.
