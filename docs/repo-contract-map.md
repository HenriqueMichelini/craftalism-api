# Repo Contract Map: craftalism-api

## Repository Role
`craftalism-api` is the authoritative backend source of truth for the economy domain. It defines the canonical server-side behavior for balances, transactions, transfers, idempotency semantics, incident persistence, and API error behavior.

## Owned Contracts
- `market-contract`
  - Owns canonical market snapshot, quote, execute, and quote lifecycle behavior
  - Owns authoritative market rejection/status semantics for consumers
- `transfer-flow`
  - Owns the canonical transfer endpoint and transfer execution semantics
  - Owns atomicity guarantees for debit/credit/ledger behavior
- `transaction-routes`
  - Owns canonical transaction route structure and backward-compatibility behavior
- `error-semantics`
  - Owns authoritative API error/status mapping for consumers
- `idempotency`
  - Owns retry-safety semantics for transfer operations
- `incident-recording`
  - Owns structured persistence/queryability of critical operational incidents

## Consumed Contracts
- `auth-issuer`
  - Must validate tokens using the canonical issuer/JWKS expectations
- `ci-cd`
  - Must comply with required backend quality gates
- `testing`
  - Must provide sufficient test coverage for all owned contracts
- `documentation`
  - Must keep implementation docs aligned with actual API behavior and shared ecosystem contracts

## Local-Only Responsibilities
- Layering and transactional boundary clarity
- Persistence correctness
- Security policy implementation and documentation
- Route/controller/service/repository cohesion
- Database migrations and backward compatibility where needed

## Out of Scope
- Plugin-side fallback strategy
- Minecraft command UX
- Dashboard UI behavior
- Auth-server token issuance internals
- Deployment orchestration logic beyond consumed configuration/runtime expectations

## Compliance Questions
- Are all owned contracts fully implemented and authoritative?
- Are consumer-facing routes and semantics stable, documented, and test-covered?
- Is issuer validation aligned with the canonical issuer contract?
- Are CI/CD, testing, and docs strong enough for a contract-owning service?

## Success Signal
This repo is compliant when it acts as a stable and well-documented source of truth that other repos can consume without ambiguity or drift.
