# CARD-001: Add Market Trade History Persistence

## Status

planned

## Objective

Add durable persistence for immutable market trade history records.

## Context

Market trade history needs its own committed execution record instead of reading from quotes, because quote rows also include pending, consumed-with-rejection, expired, and invalidated lifecycle states.

Depends on: none.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../market-pressure-ladder/contract.md`

## Expected Behavior

The repository has a migration, JPA model, and repository surface capable of storing completed market execution records with the fields required by the trade history contract.

## Acceptance Criteria

- [ ] A Flyway migration creates a market trade history table with fields for id, player UUID, item id, side, quantity, unit price, total price, currency, snapshot version, and executed-at timestamp.
- [ ] The table supports efficient filtering by player UUID, item id, side, and executed-at range.
- [ ] A JPA entity maps the table without coupling trade history rows to mutable quote lifecycle state.
- [ ] A repository exists for creating and reading trade history records.
- [ ] Migration tests or schema validation cover the new table and expected indexes.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketTradeHistory.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketTradeHistoryRepository.java
java/src/main/resources/db/migration/V17__create_market_trade_history_table.sql
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/migration/MarketTradeHistoryMigrationTest.java
```

## Constraints

- Do not derive trade history reads from `market_quotes`.
- Do not change market quote schema or quote lifecycle semantics.
- Do not change execute behavior in this card.
- Do not expose new API endpoints in this card.
- Do not introduce architectural changes beyond the persistence surface required for trade history.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.migration.MarketTradeHistoryMigrationTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Writing trade history records during execute.
- Trade history list or detail endpoints.
- Dashboard or client changes.
- Backfilling historical trade history from quote rows.

## Suggested Commit Message

`feat(craftalism-api): add market trade history persistence`

## Completion Notes

