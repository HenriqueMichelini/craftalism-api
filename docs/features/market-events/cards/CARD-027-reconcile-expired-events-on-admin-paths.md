# CARD-027: Reconcile Expired Events On Admin Paths

## Status

completed

## Objective

Reconcile elapsed active market events before admin lifecycle operations so dashboard-visible state and the database active-slot guard match wall-clock effectiveness.

## Context

Named market events are effective only while `status == ACTIVE`, `startedAt <= now`, and `endsAt > now`. The repository already supports an opportunistic bulk transition from elapsed `ACTIVE` rows to `EXPIRED`, including clearing `activeSlot`, but only the scheduled worker currently invokes it.

`MarketEventAdminService.updateEvent()` can save an `endsAt` value at or before the current time and immediately return a row with `status == ACTIVE` and `endReason == null`. Admin list, start, and supersede paths can also observe or retain stale durable active state until scheduler cleanup runs. Pricing and blocking reads correctly ignore elapsed rows, but admin-visible lifecycle state and the one-active-event persistence guard can lag behind wall-clock effectiveness.

## Required Reading

- `../contract.md`
- `CARD-007-add-admin-market-event-controls.md`
- `CARD-012-add-active-event-lifecycle-and-locking.md`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventAdminService.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/domain/event/MarketEventLifecycleService.java`
- `../../../../java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketEventInstanceRepository.java`

## Expected Behavior

Admin event reads and mutations opportunistically transition elapsed active events to durable `EXPIRED` state before returning or applying lifecycle-sensitive operations. Updating an event so that its `endsAt` is at or before the current time returns an expired row with `endReason == EXPIRED`, clears the active-slot guard, and does not require a scheduler tick. Existing wall-clock effectiveness rules remain unchanged.

## Acceptance Criteria

- [ ] `GET /api/dashboard/market/events` reconciles elapsed active rows before serializing admin-visible state.
- [ ] Updating an event with `endsAt <= now` returns `status == EXPIRED` and `endReason == EXPIRED`.
- [ ] Updating duration in a way that produces `endsAt <= now` returns the same expired lifecycle state.
- [ ] Manual start and supersede paths reconcile elapsed active rows before relying on the database-backed one-active-event guard.
- [ ] Expiration reconciliation clears `activeSlot` through the existing repository cleanup path.
- [ ] Reconciliation remains opportunistic; pricing and blocking correctness do not depend on cleanup having run.
- [ ] Existing cancel behavior still returns `CANCELLED` with `endReason == CANCELLED`.
- [ ] Tests cover admin list cleanup, update-to-past cleanup, replacement start after elapsed cleanup, supersede after elapsed cleanup, and unchanged cancel semantics.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventAdminService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/MarketEventAdminServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketEventAdminApiIntegrationTest.java
```

The lifecycle service or repository may change only if the existing cleanup API cannot safely support admin-path reconciliation.

## Constraints

- Do not change the effective-active query semantics.
- Do not add a scheduled cleanup policy or change scheduler cadence.
- Do not change public `/api/market/**` routes or DTOs.
- Do not change the database schema or weaken the one-active-event guard.
- Do not infer expiration in dashboard code.
- Do not change cancellation or supersession meanings.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventAdminServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleServiceTest
```

Run from `java/`.

## Out of Scope

- Dashboard UI controls
- Public event history
- Event pricing, drift, blocking, quote, or scheduler behavior changes
- Database migration changes
- Audit metadata schema redesign

## Completion Notes

- Admin list, manual start, supersede, update, and cancel paths now opportunistically reconcile elapsed active events through the existing lifecycle cleanup API.
- Update responses reload persisted state after cleanup so edits that produce `endsAt <= now` return `EXPIRED` with end reason `EXPIRED`.
- The repository cleanup query now flushes before its bulk update and clears the persistence context afterward so admin responses do not retain stale managed state.
- Added API integration coverage for list cleanup, direct end-time cleanup, duration cleanup, replacement start after cleanup, supersede after cleanup, and active-slot clearing. Existing cancel coverage continues to verify unchanged `CANCELLED` semantics.
- Validation: `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventAdminServiceTest --tests io.github.HenriqueMichelini.craftalism.api.controller.DashboardMarketEventAdminApiIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleServiceTest` passed from `java/`.
