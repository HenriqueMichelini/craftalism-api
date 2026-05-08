# CARD-019: Extract Market Quote Orchestration

## Status

completed

## Objective

Move quote request orchestration out of `MarketService` into a focused quote component.

## Context

RefactorFirst identified `MarketService.java` as highly coupled. The quote path currently combines market-open checks, regeneration reads, snapshot validation, player resolution, rate limiting, item lookup, availability checks, trade planning, quote token generation, and quote storage.

This card reduces `MarketService` coupling without changing quote behavior.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Quote responses, rejection codes, rate limiting, stale snapshot handling, quote TTL, and stored quote fields remain unchanged.

## Acceptance Criteria

- [ ] `MarketService.quote(...)` delegates quote orchestration to a focused component.
- [ ] Quote rejection behavior remains unchanged for stale snapshot, unknown item, blocked item, non-operating item, invalid quantity, rate limit, and pressure bounds.
- [ ] Stored quote values remain unchanged.
- [ ] Quote token generation remains opaque and single-use behavior is preserved.
- [ ] Focused tests cover unchanged quote behavior.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketQuoteService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketControllerTest.java
```

## Constraints

- Do not change quote token format.
- Do not change quote TTL behavior.
- Do not change stale quote semantics.
- Do not change pressure planning semantics.
- Do not change public API DTOs or endpoint contracts.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Execute quote consumption changes.
- Snapshot DTO changes.
- Balance settlement changes.
- Persistence or migration changes.

## Suggested Commit Message

`refactor(craftalism-api): extract market quote orchestration`

## Completion Notes

- Extracted quote orchestration into `MarketQuoteService`.
- `MarketService.quote(...)` now delegates to the focused quote service.
- Added focused stored-quote coverage in `MarketQuoteServiceTest`.
- Preserved quote response fields, rejection semantics, quote TTL, rate limiting, snapshot validation, and pressure planning behavior.
- Validation passed from `java/`: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteServiceTest`.
- Validation passed from `java/`: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest`.
