# CARD-003: Document Transfer Idempotency Header

## Status

implemented

## Objective

Expose the required transfer `Idempotency-Key` header contract clearly in API documentation and focused tests.

## Context

Audit evidence confirmed `POST /api/balances/transfer` requires the `Idempotency-Key` header, but the controller operation documents the request body and responses without an explicit OpenAPI parameter for the required header or its replay/conflict behavior.

The API already enforces the header through `TransferService`; this card scopes documentation and contract-test clarity only.

## Required Reading

- `../contract.md`
- `../../balance-integrity/contract.md`
- `../../../repo-contract-map.md`
- `../../../repo-requirement-pack.md`

## Expected Behavior

The transfer endpoint OpenAPI metadata clearly documents `Idempotency-Key` as a required header for successful transfer requests, including replay and conflict semantics at a high level.

Runtime transfer behavior remains unchanged.

## Acceptance Criteria

- [ ] `BalanceController.transfer(...)` includes explicit OpenAPI documentation for the `Idempotency-Key` request header.
- [ ] The documented header is marked required.
- [ ] The operation documentation mentions same-key replay returns the original successful transfer response.
- [ ] The operation documentation mentions same-key different-payload conflicts return the existing conflict response.
- [ ] Existing transfer runtime behavior and response DTOs remain unchanged.
- [ ] Focused controller or integration tests continue to prove missing, replayed, and conflicting idempotency-key behavior.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceController.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/BalanceTransferIntegrationTest.java
```

## Constraints

- Do not change `Idempotency-Key` header spelling.
- Do not change transfer request or response DTO shapes.
- Do not change idempotency persistence behavior.
- Do not change transfer replay or conflict semantics.
- Do not introduce client-specific documentation outside this API-owned contract.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceTransferIntegrationTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Changing transfer implementation.
- Changing idempotency conflict status codes.
- Changing incident recording behavior.
- Adding generated OpenAPI snapshot files.
- Updating dashboard or other consumers.

## Completion Notes

- Documented `Idempotency-Key` as a required OpenAPI header for `POST /api/balances/transfer`.
- Documented same-key replay and same-key different-payload conflict semantics at the operation level.
- Added focused controller reflection coverage while preserving runtime transfer behavior.
- Validation passed with the card command from `java/`.
