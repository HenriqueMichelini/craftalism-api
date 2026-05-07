# CARD-014: Return Market Rejection For Invalid Quantity

## Status

planned

## Objective

Make invalid market quote and execute quantities return the canonical market rejection payload with `INVALID_QUANTITY`.

## Context

Audit evidence confirmed that `MarketQuoteRequestDTO.quantity` and `MarketExecuteRequestDTO.quantity` use `@Positive`, so HTTP requests with `quantity <= 0` are rejected by generic validation before `MarketService` can emit `MarketRejectionCode.INVALID_QUANTITY`.

The market contract lists `INVALID_QUANTITY` as a stable machine-readable market rejection code. This card explicitly scopes the public API error behavior correction for market quote and execute requests.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

`POST /api/market/quotes` and `POST /api/market/execute` return the standard market rejection response for zero or negative quantities:

- `status`: `REJECTED`
- `code`: `INVALID_QUANTITY`
- `message`: quantity-specific and stable enough for clients to understand
- `snapshotVersion`: current backend market snapshot version

Other malformed request-body validation errors may continue to use the global validation response unless this card explicitly touches the market quantity path.

## Acceptance Criteria

- [ ] Quote requests with `quantity = 0` return the market rejection payload with code `INVALID_QUANTITY`.
- [ ] Quote requests with `quantity < 0` return the market rejection payload with code `INVALID_QUANTITY`.
- [ ] Execute requests with `quantity = 0` return the market rejection payload with code `INVALID_QUANTITY`.
- [ ] Execute requests with `quantity < 0` return the market rejection payload with code `INVALID_QUANTITY`.
- [ ] Invalid quantity responses include the current market `snapshotVersion`.
- [ ] Existing validation behavior for missing item id, side, quote token, or snapshot version is not changed unless required by the quantity fix.
- [ ] Existing successful quote and execute behavior is unchanged.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketQuoteRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketExecuteRequestDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
```

Adjust handler or controller code only if the DTO/service path is insufficient to preserve the market rejection contract.

## Constraints

- This card explicitly scopes a public API error behavior correction for market quantity validation.
- Do not change market DTO field names or response shapes except for the invalid quantity error path.
- Do not change pricing, quote token generation, quote lifecycle, pressure mutation, regeneration, or balance settlement.
- Do not implement client behavior in this repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest
```

Run from `java/`.

Fallback for local iteration:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest
```

## Out of Scope

- Adding rate limiting.
- Changing non-market validation error semantics.
- Changing market request or response DTO field names.
- Updating `craftalism-market` or any other consumer repository.

## Completion Notes

Leave empty until implemented.
