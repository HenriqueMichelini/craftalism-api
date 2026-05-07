# CARD-016: Implement Market Rate Limited Rejection Policy

## Status

planned

## Objective

Implement a bounded market quote and execute rate-limit policy that emits the documented `RATE_LIMITED` rejection code.

## Context

Audit evidence confirmed that `RATE_LIMITED` is documented in the market contract and present in `MarketRejectionCode`, but no runtime code emits it. This leaves the owned market rejection contract partially implemented.

This card scopes the backend policy and implementation. Consumers should only depend on the stable rejection code and response shape, not on internal limiter mechanics.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Market quote and execute requests are rate limited by an explicit backend-owned policy. When the limit is exceeded, the endpoint returns the standard market rejection payload with code `RATE_LIMITED` and the current market `snapshotVersion`.

The initial policy must be configurable and deterministic enough to test without relying on wall-clock flakiness.

## Acceptance Criteria

- [ ] A configurable rate-limit policy exists for `POST /api/market/quotes`.
- [ ] A configurable rate-limit policy exists for `POST /api/market/execute`.
- [ ] Exceeding the quote limit returns market rejection code `RATE_LIMITED`.
- [ ] Exceeding the execute limit returns market rejection code `RATE_LIMITED`.
- [ ] Rate-limited responses include the current market `snapshotVersion`.
- [ ] Defaults preserve current practical behavior unless a configured limit is exceeded.
- [ ] Tests cover allowed requests and rejected requests without sleeping.
- [ ] Security is not weakened and authenticated player resolution continues to work as before.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/resources/application.properties
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
```

Add a small market rate-limit helper/service only if it keeps the policy testable and contained.

## Constraints

- This card explicitly scopes a public API rejection behavior addition for the existing `RATE_LIMITED` code.
- Do not change existing market rejection payload shape.
- Do not change quote token generation, quote persistence, quote expiry, pressure pricing, pressure mutation, or balance settlement.
- Do not add distributed infrastructure dependencies unless explicitly approved by a later card.
- Do not implement consumer behavior in this repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

Fallback for local iteration:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest
```

## Out of Scope

- Global API rate limiting outside market quote and execute endpoints.
- Dashboard or Minecraft client messaging.
- Redis or distributed limiter infrastructure.
- Changing authentication or authorization requirements.

## Completion Notes

Leave empty until implemented.
