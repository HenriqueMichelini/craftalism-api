# CARD-010: Persist Quote Pricing Context

## Status

planned

## Objective

Persist immutable quote pricing context metadata so a quote is a durable price promise with enough internal data for audit and debugging.

## Context

Market quotes already store authoritative `unitPrice` and `totalPrice`. Market Events require additional context so operators can understand which drift and named event conditions produced a quote without recomputing current market prices during execute.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Each stored quote preserves the immutable unit and total price used for settlement and records lightweight context metadata such as pricing context version, base pressure position, drift value or revision, event instance id, and event effect version when applicable. Execute settles using the stored quote price after validity checks.

## Acceptance Criteria

- [ ] Quote persistence includes a pricing context version.
- [ ] Quote persistence records the pressure position used at quote creation.
- [ ] Quote persistence can record drift value or revision used at quote creation.
- [ ] Quote persistence can record named event instance/effect identity when an event affects the quote.
- [ ] Quote read/write mapping preserves the context fields.
- [ ] Execute does not recompute current drift or event-adjusted prices as the source of settlement truth.
- [ ] Migration tests cover non-null/default behavior for existing quote rows where applicable.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/
java/src/main/resources/db/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not apply drift or named event modifiers in this card.
- Do not expose internal pricing context through public quote or execute responses.
- Do not use pricing context fields to bypass quote expiry, single-use, or availability checks.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketQuoteMigrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteServiceTest
```

Run from `java/`.

## Out of Scope

- Drift state.
- Named event persistence.
- Public API shape changes.
- Scheduler or admin APIs.

## Completion Notes
