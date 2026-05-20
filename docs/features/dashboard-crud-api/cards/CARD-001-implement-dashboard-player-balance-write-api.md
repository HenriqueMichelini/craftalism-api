# CARD-001: Implement Dashboard Player And Balance Write API

## Status

completed

## Objective

Implement canonical player and balance write endpoints needed by dashboard create, edit, and delete modals.

## Context

The dashboard already has local modal behavior for player and balance CRUD but needs backend write semantics before persisting modal actions.

## Required Reading

- `../contract.md`
- `../../balance-integrity/contract.md`

## Expected Behavior

Players can be created, renamed, and deleted through canonical player routes, while UUIDs remain immutable. Balances can be created, amount-updated, and deleted through canonical balance routes, where the balance UUID is the player UUID and amounts are scaled non-negative integers.

## Acceptance Criteria

- [ ] `POST /api/players` remains the canonical player create route and returns `201`, `Location`, and the created player.
- [ ] `PATCH /api/players/{uuid}` updates only the player name and returns the updated player.
- [ ] `DELETE /api/players/{uuid}` deletes an unreferenced player and returns `204`.
- [ ] Player write validation and duplicate conflicts return the documented `ProblemDetail` shape and status codes.
- [ ] `POST /api/balances` remains the canonical balance create route and validates that the referenced player exists.
- [ ] `PATCH /api/balances/{uuid}` updates the balance amount and returns the updated balance.
- [ ] `DELETE /api/balances/{uuid}` deletes an existing balance and returns `204`.
- [ ] Balance write validation, unknown player references, duplicate balances, and invalid amounts return the documented `ProblemDetail` shape and status codes.
- [ ] Existing transfer, deposit, withdraw, and `PUT /api/balances/{uuid}/set` behavior remains available.
- [ ] Focused controller/service/API tests cover create, update, delete, and validation or conflict errors.

## Expected Files to Change

```text
docs/features/index.md
docs/features/dashboard-crud-api/contract.md
docs/features/dashboard-crud-api/cards/CARD-001-implement-dashboard-player-balance-write-api.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not change persistence schema.
- Do not change dashboard frontend code.
- Do not change transfer, market, transaction, idempotency, or incident semantics.
- Do not enforce UUID7 unless separately scoped by a future contract change.

## Validation Commands

```bash
./gradlew test --tests '*PlayerServiceTest' --tests '*BalanceServiceTest' --tests '*PlayerControllerTest' --tests '*BalanceControllerTest' --tests '*DashboardCrudApiIntegrationTest' --tests '*GlobalExceptionHandlerContractTest'
```

Fallback if the filtered command is unavailable:

```bash
./gradlew test
```

## Out of Scope

- Dashboard frontend API client changes.
- UUID7-only backend enforcement.
- Balance response field rename from `uuid` to `playerUuid`.
- Schema or migration changes.
- Transfer, market, transaction, idempotency, or incident behavior changes.

## Completion Notes

- Implemented `PATCH /api/players/{uuid}` and `DELETE /api/players/{uuid}`.
- Implemented `PATCH /api/balances/{uuid}` and `DELETE /api/balances/{uuid}`.
- Kept existing player and balance create routes unchanged.
- Documented canonical dashboard CRUD API contracts in `docs/features/dashboard-crud-api/contract.md`.
- Validated with the filtered card command and full `./gradlew test`.
