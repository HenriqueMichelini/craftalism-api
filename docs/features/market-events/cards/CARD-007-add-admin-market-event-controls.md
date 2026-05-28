# CARD-007: Add Admin Market Event Controls

## Status

planned

## Objective

Add admin controls to view, create, edit, cancel, and supersede market events with auditability.

## Context

Admin is sovereign over events, but manual actions need warnings and auditability. Admin source should be internal metadata, not normally player-facing.

This card depends on a dedicated event-admin authorization boundary. Generic `SCOPE_api:write` is not sufficient for event admin mutations.

## Required Reading

- `../contract.md`
- `CARD-012-add-active-event-lifecycle-and-locking.md`
- `CARD-013-add-event-admin-authorization-boundary.md`

## Expected Behavior

Authorized event-admin paths can inspect active and historical event data, manually start events, cancel events, and supersede active events. Superseded events end immediately with reason `SUPERSEDED`.

## Acceptance Criteria

- [ ] Admin can list active and recent/internal event instances with full internal metadata.
- [ ] Admin can manually start an event from a template or explicit event request.
- [ ] Admin can cancel an active event with a reason.
- [ ] Admin can supersede an active event; the previous event ends with reason `SUPERSEDED`.
- [ ] Admin operations record source, actor if available, timestamps, exact values, target, reason, and before/after state.
- [ ] Admin can bypass selected scheduler guardrails only through explicit manual paths.
- [ ] Admin mutation routes require the event-admin authority established by `CARD-013`.
- [ ] Internal admin metadata is not exposed through public `GET /api/market/**` endpoints.
- [ ] Public market snapshot does not reveal whether an event was admin-triggered.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
```

## Constraints

- Do not weaken existing admin/dashboard authentication or authorization.
- Do not rely on generic `SCOPE_api:write` as sufficient for event admin mutations.
- Do not expose admin metadata through public market endpoints.
- Do not implement client UI.
- Do not silently delete event history.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketEventAdminServiceTest
```

Run from `java/`.

## Out of Scope

- Automatic scheduler changes.
- Public event history.
- Player notifications.

## Completion Notes
