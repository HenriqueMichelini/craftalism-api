# CARD-032: Retire Rarity-Dependent Event Rules

## Status

completed

## Objective

Remove scheduler and template-validation behavior that depends on `MarketEventRarity`, replacing those decisions with explicit template fields and rules.

## Context

Market event rarity is being removed from the API/domain. Before deleting the persisted/API field, runtime behavior should stop treating rarity as an independent policy input.

## Required Reading

- `../contract.md`

## Expected Behavior

Automatic scheduling eligibility is controlled by `automaticEnabled`, positive `automaticWeight`, `blockingAllowed`, cooldown, scope, and target eligibility. Template validation authorizes blocking templates through explicit fields: neutral effect basis-point range, `blockingAllowed`, `automaticEnabled == false`, and item scope. No scheduler or validation branch treats medium, rare, or extra-rare as special.

## Acceptance Criteria

- [ ] `MarketEventSelectionPolicy` no longer imports or checks `MarketEventRarity`.
- [ ] Scheduler automatic candidates use `automaticEnabled`, `automaticWeight`, and `blockingAllowed` without an extra-rare feature flag or rare blocking branch.
- [ ] Scheduler configuration no longer exposes or consumes `craftalism.market-events.scheduler.automatic-extra-rare-enabled`.
- [ ] Template validation no longer accepts rarity as an input and no longer rejects configurations because of rarity.
- [ ] Blocking-template validation uses explicit fields only: item scope, manual selection, `blockingAllowed`, and neutral `10000` effect bounds.
- [ ] Focused scheduler and template-validation tests cover the explicit rules and remove rarity-specific expectations.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/domain/event/MarketEventSelectionPolicy.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/scheduling/MarketEventScheduler.java
java/src/main/resources/application.properties
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventTemplateTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/infrastructure/scheduling/MarketEventSchedulerTest.java
```

## Constraints

- Do not remove the rarity DTO, entity, enum, or database columns in this card.
- Do not derive, default, or fabricate rarity from automatic weight or any other field.
- Do not introduce a replacement bucket-like field.
- Do not change event pricing, lifecycle, lease, cooldown, or weighted-selection math except where the previous rarity branches affected eligibility.
- Do not modify dashboard frontend code.

## Validation Commands

Run from `java/`:

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateTest --tests io.github.HenriqueMichelini.craftalism.api.market.infrastructure.scheduling.MarketEventSchedulerTest
```

If targeted tests cannot run, use:

```bash
rtk ./gradlew test
```

## Out of Scope

- Removing rarity from public or dashboard API payloads.
- Removing rarity from `MarketEventTemplate` or `MarketEventInstance`.
- Removing rarity persistence columns or migrations.
- Deleting `MarketEventRarity`.
- Dashboard consumption of the rarity-free API.

## Suggested Commit Message

`refactor(market-events): retire rarity dependent event rules`

## Completion Notes

- Removed rarity-dependent scheduler eligibility and template-validation branches.
- Automatic scheduler candidates now depend on `automaticEnabled`, positive `automaticWeight`, and `blockingAllowed`, with cooldown and target eligibility unchanged.
- Blocking-template validation now uses explicit fields only: item scope, manual selection, `blockingAllowed`, and neutral `10000` effect bounds.
- Validated with the targeted CARD-032 scheduler/template test command and with the full test suite during CARD-033 validation.
