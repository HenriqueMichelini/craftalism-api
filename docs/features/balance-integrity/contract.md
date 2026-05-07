# Balance Integrity Contract

## Purpose

Define repo-local backend ownership, stable rules, and validation boundaries for authoritative balance invariants.

## Repository Ownership

`craftalism-api` owns authoritative balance persistence, balance mutation semantics, transfer settlement safety, and balance-related API error behavior.

## Goals

- Preserve non-negative balances across controller, service, and persistence boundaries.
- Keep balance mutations transactional and explicit.
- Keep transfer and market settlement behavior aligned with authoritative balance invariants.
- Provide backend tests that verify service-level invariants independently from DTO validation.

## Non-Goals

- Do not implement client UI or command behavior in this repository.
- Do not redefine cross-repo economy rules outside this API-owned backend behavior.
- Do not change currency model or introduce multi-currency balances unless explicitly scoped by a future card.

## Domain Rules

- Balance amount must never be negative.
- Create and set operations allow zero or positive amounts only.
- Deposit and withdraw amounts must be positive.
- Withdraw and transfer debit operations must reject insufficient funds.
- Transfer operations must remain atomic across debit, credit, ledger, idempotency, and incident handling behavior.

## Invariants

- `balances.amount` is non-negative at the service boundary and persistence boundary.
- Service methods must not rely only on controller DTO validation for domain invariants.
- Persistence constraints are a final safeguard, not the primary user-facing validation mechanism.

## External Interfaces

- `GET /api/balances`
- `GET /api/balances/{uuid}`
- `POST /api/balances`
- `PUT /api/balances/{uuid}/set`
- `POST /api/balances/{uuid}/deposit`
- `POST /api/balances/{uuid}/withdraw`
- `POST /api/balances/transfer`
- `GET /api/balances/top`

## Cross-Feature Dependencies

- Market buy settlement consumes balances.
- Market sell settlement may create or credit balances.
- Transfer idempotency and incident recording depend on authoritative balance mutation behavior.

## Public Contract Change Rules

Changes to balance APIs, persistence schema, permissions, transfer semantics, or error semantics require explicit card scope.

## Persistence Rules

Balance persistence must retain a non-negative amount constraint.

## Error and Failure Rules

- Invalid non-positive mutation amounts must reject before persistence.
- Negative create or set amounts must reject before persistence.
- Insufficient funds must not mutate balances.
- Failed transfer settlement must not leave partial balance mutation.

## Security and Permission Rules

Do not weaken authentication, authorization, idempotency, transfer safety, market settlement safety, or incident handling while changing balance behavior.

## Validation Rules

Prefer focused service tests for balance invariants. Use controller tests when HTTP error semantics change. Use transfer or market tests when settlement behavior changes.

## Source Areas

- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/`
- `java/src/main/resources/db/migration/`

## Test Areas

- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/`
