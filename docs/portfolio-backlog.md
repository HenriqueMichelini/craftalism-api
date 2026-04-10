# Craftalism API Portfolio Backlog

Date: 2026-04-10

## Purpose

This backlog focuses on raising `craftalism-api` from MVP-correct to
portfolio-grade as the canonical backend contract owner.

Source:

- [portfolio-evolution-roadmap.md](/home/henriquemichelini/IdeaProjects/craftalism/docs/portfolio-evolution-roadmap.md)
- [repo-requirement-pack.md](/home/henriquemichelini/IdeaProjects/craftalism-api/docs/repo-requirement-pack.md)

## Now

### High priority

- Add PostgreSQL-backed CI integration tests for transfer, idempotency, and
  security-sensitive write behavior.
- Add regression tests for transfer contention and repeated idempotency-key
  submissions.
- Add rollback-path tests for partially failed transfer and incident-recording
  scenarios.
- Add API contract verification for canonical routes and RFC 9457 error payload
  shape.

### Medium priority

- Add structured logs or metrics for transfer latency, JWT auth failures, DB lock
  contention, and incident persistence failures.
- Add clearer verification around Flyway migrations in CI.

## Next

### High priority

- Add OpenAPI snapshot or compatibility checks so route and schema drift becomes
  visible in review.
- Add pagination, filtering, and sorting standards where they improve operator
  use without changing ownership boundaries.
- Add stronger auditability for sensitive writes:
  actor identity, request ID, and domain context in logs.

### Medium priority

- Add explicit compatibility and deprecation policy for route aliases and legacy
  behavior.
- Add migration tests covering realistic upgrade paths, not only fresh schema
  startup.

## Later

- Add targeted performance tests for hot paths such as balance lookups and
  transfers.
- Add read-model improvements only if they materially improve operator
  experience and stay within the current architecture.

## Done When

- The API is clearly the most trustworthy contract owner in the platform.
- Critical transfer behavior is covered by real database-backed tests.
- Contract drift is difficult to introduce silently.
