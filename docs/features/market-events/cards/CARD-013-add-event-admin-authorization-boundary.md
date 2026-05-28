# CARD-013: Add Event Admin Authorization Boundary

## Status

completed

## Objective

Define and enforce a narrower authorization boundary for market event admin mutations before adding event admin controls.

## Context

Manual market events can move prices, block items, cancel active events, and supersede scheduler decisions. Generic API write scope is too broad for these operations.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

Market event admin mutation endpoints require a dedicated event-admin authority such as `SCOPE_market:admin`, or an explicitly documented equivalent dashboard/admin authority if one exists in the wider platform. Public market endpoints and ordinary quote/execute write paths keep their existing access rules.

## Acceptance Criteria

- [ ] Event admin mutation authority is explicitly named and documented.
- [ ] Event admin mutation routes require the event-admin authority.
- [ ] Event admin read routes that expose internal metadata are not public.
- [ ] Existing public `GET /api/market/**` behavior is not broadened to expose internal event data.
- [ ] Existing quote and execute authorization is not weakened.
- [ ] Security tests cover allowed and rejected event-admin mutation requests.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/config/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
```

## Constraints

- Do not implement event admin business operations in this card.
- Do not change client UI behavior.
- Do not make internal event metadata public.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminSecurityTest
```

Run from `java/`.

## Out of Scope

- Event persistence.
- Event scheduler.
- Admin trigger/cancel/supersede service behavior.

## Completion Notes

- Enforced `/api/dashboard/market/events/**` behind the dedicated `SCOPE_market:admin` authority before generic public/read and write API matchers.
- Verified generic `SCOPE_api:write` is insufficient for event-admin mutations.
- Verified internal event-admin read routes are not public while public market routes and ordinary quote/execute authorization remain unchanged.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminSecurityTest` passed from `java/`.
