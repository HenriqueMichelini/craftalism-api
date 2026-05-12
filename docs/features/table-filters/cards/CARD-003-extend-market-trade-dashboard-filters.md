# CARD-003: Extend Market Trade Dashboard Filters

## Status

planned

## Objective

Extend the existing market trade history read API contract and implementation for dashboard table filter needs.

## Context

`GET /api/market/trades` already supports pageable filters for player, item, side, and execution time range. Dashboard table filters additionally need text match modes and total price ranges. Frontend `type` terminology is dashboard-owned and maps to canonical API `side` outside this repository.

Depends on:

- Existing completed `market-trade-history` read API.

## Required Reading

- `../contract.md`
- `../../market-trade-history/contract.md`
- `../../market-trade-history/cards/CARD-003-expose-market-trade-history-read-api.md`
- `../../../market-contract-mvp.md`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketController.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadService.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketTradeHistoryFilterDTO.java`

## Expected Behavior

`GET /api/market/trades` supports the dashboard table filter set without breaking the existing market trade history contract.

## Acceptance Criteria

- [ ] Existing filters remain supported: `playerUuid`, `itemId`, `side`, `executedFrom`, and `executedTo`.
- [ ] Text match mode behavior is defined and implemented for `playerUuid` and `itemId`.
- [ ] `minTotalPrice` and `maxTotalPrice` are defined and implemented as inclusive bounds.
- [ ] The API keeps canonical `side` terminology; dashboard-owned `type` terminology is not added as an API alias.
- [ ] Filters apply before pagination.
- [ ] Default ordering remains `executedAt,DESC`, then `id,DESC`.
- [ ] Empty results return `200` with an empty `Page` content array.
- [ ] Invalid filters return repository-standard validation errors.
- [ ] Tests cover existing filters, new filters, invalid filters, empty results, pagination, sorting, and public read access.

## Expected Files to Change

```text
docs/features/table-filters/contract.md
docs/features/market-trade-history/contract.md
docs/market-contract-mvp.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketController.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketTradeHistoryFilterDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/security/SecurityFilterChainTest.java
```

## Constraints

- Do not change market quote or execute behavior.
- Do not expose rejected, pending, expired, or quote lifecycle records as trade history.
- Do not weaken write-route authorization.
- Do not implement dashboard behavior.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeHistoryReadServiceTest --tests io.github.HenriqueMichelini.craftalism.api.security.SecurityFilterChainTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Dashboard implementation
- Transactions filters
- Market quote or execute behavior
- Authentication rollout

## Completion Notes
