# CARD-009: Split Snapshot Freshness From Quote Execution Validity

## Status

completed

## Objective

Change market quote execution semantics so `snapshotVersion` remains a browse and quote-creation freshness token, while already-issued quotes remain executable until expiry unless the quote itself or effective item availability invalidates execution.

## Context

Market Events introduce time-based price modifiers. A named price event may end after quote creation, but the quote price must remain valid until quote expiry. The current execute flow rejects when the current snapshot version differs from the stored quote snapshot version, which would make event-priced quotes unsafe.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Quote creation still requires the request `snapshotVersion` to match the current market snapshot. Once a quote is issued, execute validates quote token identity, status, expiry, single-use lifecycle, request fields, item existence, and effective availability. Execute does not reject solely because the current market snapshot version changed after quote creation.

## Acceptance Criteria

- [ ] Quote creation still rejects stale browse snapshots.
- [ ] Execute validates quote token status, expiry, single-use transition, player, item, side, quantity, and request snapshot token identity against the stored quote.
- [ ] Execute does not compare the stored quote snapshot version to the current market snapshot version as a standalone stale-quote rejection.
- [ ] Execute still rejects if the item no longer exists, is not operating, or is effectively blocked at execution time.
- [ ] Failed settlement still does not mutate pressure, balances, or trade history.
- [ ] Existing quote expiration and single-use semantics are preserved.
- [ ] Tests cover a quote executing successfully after unrelated market snapshot changes that do not affect effective availability.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
```

## Constraints

- Do not apply drift or named event modifiers in this card.
- Do not weaken quote token single-use behavior.
- Do not allow execution when the item is effectively blocked or not operating.
- Do not change pressure mutation direction or settlement behavior.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

## Out of Scope

- Drift persistence.
- Named event persistence.
- Pricing modifiers.
- Scheduler or admin APIs.

## Completion Notes

- Removed execute-time rejection based only on current snapshot version differing from the stored quote snapshot version.
- Kept quote creation stale-snapshot checks and execute request-vs-stored quote identity validation.
- Added regression coverage for execution after an unrelated snapshot-version change.
- Updated integration coverage so same-item market state movement no longer fails solely because the snapshot token moved when the stored quote price still matches.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest` passed from `java/`.
