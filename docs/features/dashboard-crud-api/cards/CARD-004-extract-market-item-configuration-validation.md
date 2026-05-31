# CARD-004: Extract Market Item Configuration Validation

## Status

completed

## Objective

Move dashboard market-item pressure configuration validation into a focused package-private validator.

## Context

`DashboardMarketItemService` currently coordinates CRUD persistence, category lookup, default values, mutation mapping, API-owned timestamps, drift initialization, derived projection recomputation, delete guards, and pressure configuration validation. The validation block is cohesive and independently testable.

This is a behavior-preserving SRP refactor. Mutation orchestration remains in the service.

## Required Reading

- `../contract.md`
- `../../market-pressure-ladder/contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Dashboard market-item create and update operations preserve existing pressure validation rules, error messages, derived projections, timestamps, drift initialization, and persistence behavior while configuration validation is delegated to a focused validator.

## Acceptance Criteria

- [ ] A package-private market-item configuration validator owns the existing pressure configuration checks.
- [ ] `DashboardMarketItemService` delegates validation before projection recomputation and persistence.
- [ ] Existing validation rules and `MarketItemValidationException` messages remain unchanged.
- [ ] Create defaults, update behavior, API-owned timestamps, drift initialization, and derived projection recomputation remain unchanged.
- [ ] Focused validator tests cover each invalid configuration branch and a valid configuration.
- [ ] Existing dashboard market-item CRUD integration tests pass.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DashboardMarketItemService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketItemConfigurationValidator.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketItemConfigurationValidatorTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketItemCrudApiIntegrationTest.java
```

## Constraints

- Do not change dashboard market-item DTOs, API contracts, schema, or persistence mappings.
- Do not change pressure pricing, regeneration, or projection rules.
- Do not add a general-purpose validator framework.
- Do not combine this card with request-to-entity assembler extraction.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketItemConfigurationValidatorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest
```

Run from `java/`.

## Out of Scope

- Extracting market-item request assembly.
- Changing CRUD endpoint behavior.
- Adding new validation rules.
- Refactoring catalog seed construction.

## Suggested Commit Message

`refactor(craftalism-api): extract market item configuration validation`

## Completion Notes

- Extracted the existing pressure configuration checks into package-private `MarketItemConfigurationValidator`.
- Kept dashboard market-item mutation orchestration in `DashboardMarketItemService`, delegating validation before timestamp, drift initialization, projection recomputation, and persistence.
- Added focused validator tests for valid configuration and every preserved validation branch.
- Verified with:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketItemConfigurationValidatorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketItemCrudApiIntegrationTest
```

Result: `BUILD SUCCESSFUL`.
