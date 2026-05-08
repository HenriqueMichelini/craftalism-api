# CARD-001: Enforce Nonnegative Create And Set In Service

## Status

planned

## Objective

Reject negative balance create and set amounts at the service boundary before persistence.

## Context

Audit evidence confirmed that `BalanceService.createBalance` and `BalanceService.setBalance` do not guard negative amounts. DTO validation rejects negative HTTP requests, and the database has `CHECK (amount >= 0)`, but service-level callers can still produce invalid domain state or persistence exceptions instead of domain errors.

This card scopes a defense-in-depth domain invariant fix for balance service methods.

## Required Reading

- `../contract.md`
- `../../../repo-contract-map.md`
- `../../../repo-requirement-pack.md`

## Expected Behavior

`BalanceService.createBalance` and `BalanceService.setBalance` reject negative amounts with the existing invalid-amount domain error before saving.

Zero remains valid for create and set. Existing positive create and set behavior remains unchanged.

## Acceptance Criteria

- [ ] `createBalance` rejects negative initial amounts before calling `repository.save`.
- [ ] `setBalance` rejects negative amounts before calling `repository.save`.
- [ ] `createBalance` still allows zero and positive initial amounts.
- [ ] `setBalance` still allows zero and positive amounts.
- [ ] Existing deposit, withdraw, transfer, and top-balance behavior is unchanged.
- [ ] Existing DTO validation remains in place.
- [ ] Tests no longer document negative service-level set as accepted behavior.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/BalanceServiceTest.java
```

Add controller tests only if the implementation changes HTTP error behavior.

## Constraints

- Do not change balance API routes or DTO field names.
- Do not change database schema.
- Do not change transfer idempotency, incident recording, market settlement, or transaction ledger behavior.
- Do not weaken existing persistence constraints.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.BalanceServiceTest
```

Run from `java/`.

If any controller error behavior changes, also run:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.BalanceControllerTest
```

## Out of Scope

- Changing public balance API shapes.
- Changing transfer semantics.
- Changing market settlement behavior.
- Adding new balance endpoints.
- Reworking transaction ledger behavior.

## Completion Notes

- Implemented service-level negative amount guards for `createBalance` and `setBalance`.
- Added focused service tests for negative create and set rejection before persistence.
- Validation passed: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.BalanceServiceTest` from `java/`.
