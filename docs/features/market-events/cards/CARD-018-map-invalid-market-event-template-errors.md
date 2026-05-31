# CARD-018: Map Invalid Market Event Template Errors

## Status

completed

## Objective

Return a structured client error when an admin market-event request references an unknown template instead of surfacing an internal server error.

## Context

Runtime investigation on 2026-05-31 reproduced `POST /api/dashboard/market/events` returning `500` because `MarketEventAdminService.template()` throws `IllegalArgumentException` for an unknown template id and the generic exception handler maps it to an internal error.

## Required Reading

- `../contract.md`
- `../../../../../craftalism/docs/contracts/error-semantics.md`

## Expected Behavior

Admin market-event create and supersede requests that reference an unknown template return a structured validation-style `400 Bad Request` response with no event mutation. Unexpected exceptions continue to return the existing internal-error response.

## Acceptance Criteria

- [ ] Unknown market event template ids produce a market-event-specific validation exception.
- [ ] The global exception handler maps that exception to the existing structured validation ProblemDetail response.
- [ ] Admin create integration coverage verifies an unknown template returns `400 Bad Request`.
- [ ] The response does not expose internal exception details.
- [ ] No event instance is persisted for the rejected request.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketEventAdminService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventAdminApiIntegrationTest.java
```

## Constraints

- This card explicitly changes admin API error semantics for unknown market event template ids from `500` to structured validation-style `400`.
- Do not change successful create or supersede request shapes.
- Do not change template persistence, startup seeding, scheduler selection, event lifecycle, pricing, or blocking behavior.
- Do not weaken the generic unexpected-exception handler.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest
```

Run from `java/`.

## Out of Scope

- Startup template seeding.
- Dashboard template discovery or selector UI.
- Error mapping for unrelated resources.

## Completion Notes

- Added a market-event-specific template validation exception and mapped it to the structured validation ProblemDetail response.
- Resolved supersede templates before ending an active event so rejected replacements do not mutate persisted event state.
- Added integration coverage for rejected create and supersede requests and validated with the card's targeted Gradle test command.
