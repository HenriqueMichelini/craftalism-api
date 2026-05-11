# CARD-003: Expose Market Trade History Read API

## Status

planned

## Objective

Expose read-only API endpoints for paged, filterable market trade history and detail lookup.

## Context

Dashboard and ops consumers need market-specific trade history after API response shape and endpoint semantics are stable. These endpoints are read-only but must require `api:read`, unlike the public market snapshot.

Depends on:

- `CARD-001-add-market-trade-history-persistence.md`
- `CARD-002-record-successful-market-executions.md`

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`
- `../../market-pressure-ladder/contract.md`
- `CARD-001-add-market-trade-history-persistence.md`
- `CARD-002-record-successful-market-executions.md`

## Expected Behavior

`GET /api/market/trades` returns a pageable list of committed successful market executions filtered by optional `playerUuid`, `itemId`, `side`, `executedFrom`, and `executedTo` parameters. `GET /api/market/trades/{id}` returns one trade history record by id. Both endpoints require `api:read`.

## Acceptance Criteria

- [ ] `GET /api/market/trades` returns pageable trade history records with `id`, `playerUuid`, `itemId`, `side`, `quantity`, `unitPrice`, `totalPrice`, `currency`, `snapshotVersion`, and `executedAt`.
- [ ] List filters for `playerUuid`, `itemId`, `side`, `executedFrom`, and `executedTo` are optional and composable.
- [ ] `executedFrom` and `executedTo` apply inclusive timestamp bounds.
- [ ] List responses use the repository-standard Spring `Page<MarketTradeHistoryDTO>` JSON shape and support zero-based `page`, `size`, and repeated `sort=property,direction` query parameters.
- [ ] Default list ordering is newest first by `executedAt,DESC`, then `id,DESC`.
- [ ] `GET /api/market/trades/{id}` returns the matching trade history record or repository-standard not-found behavior.
- [ ] Both endpoints require `api:read` and are not permitted by the broad public GET API rule.
- [ ] `GET /api/market/snapshot` remains public.
- [ ] Quote and execute authorization requirements remain unchanged.
- [ ] Controller, service, repository, security, and contract integration tests cover list, filters, detail lookup, and access control.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketController.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketTradeHistoryDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketTradeHistoryFilterDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketTradeHistoryRepository.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/SecurityConfig.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeHistoryReadServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/security/SecurityFilterChainTest.java
```

## Constraints

- Do not make all market GET endpoints authenticated; preserve public snapshot behavior.
- Do not weaken write-scope requirements for POST, PUT, PATCH, or DELETE `/api/**` routes.
- Do not expose quote tokens or quote lifecycle records through trade history.
- Do not mutate market, quote, balance, or transfer state from read endpoints.
- Do not implement dashboard or client changes.

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

- Dashboard or client cards.
- Write-side trade history persistence.
- Changing snapshot, quote, or execute response shapes.
- Adding public access to trade history.

## Suggested Commit Message

`feat(craftalism-api): expose market trade history reads`

## Completion Notes
