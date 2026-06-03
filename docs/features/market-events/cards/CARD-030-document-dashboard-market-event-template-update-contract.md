# CARD-030: Document Dashboard Market Event Template Update Contract

## Status

completed

## Objective

Record the API-owned dashboard/admin contract for updating market event templates.

## Context

After `CARD-029` adds the backend update route, downstream dashboard work needs a stable contract for method, path, request shape, response shape, authorization, validation ownership, and template identity behavior. This documentation is the API-side source of truth that the dashboard can consume before implementing its update client and edit modal.

## Required Reading

- `../contract.md`
- `CARD-022-add-dashboard-market-event-template-api.md`
- `CARD-029-add-dashboard-market-event-template-update-api.md`

## Expected Behavior

The market-events feature contract records the dashboard/admin template update route and DTO contract added by `CARD-029`. It clearly states that `craftalism-api` owns validation, persistence, authorization, scheduler semantics, pricing semantics, and lifecycle semantics, while out-of-repo dashboards may only submit authored update requests and render returned template rows.

## Acceptance Criteria

- [ ] `docs/features/market-events/contract.md` records the update method and path for market event templates.
- [ ] The contract records the complete update request shape.
- [ ] The contract records the update response shape or explicitly states that it reuses the existing template response row.
- [ ] The contract records whether `templateId` is path-bound and immutable or changeable through the request.
- [ ] The contract records the `SCOPE_market:admin` authorization boundary.
- [ ] The contract records validation and error ownership at the API boundary without duplicating all implementation details from service code.
- [ ] The contract states that delete behavior remains out of scope unless a later card explicitly adds it.
- [ ] The contract states that frontend/dashboard repositories must not calculate template validation, persistence, scheduler behavior, pricing behavior, or lifecycle semantics locally.

## Expected Files to Change

```text
docs/features/market-events/contract.md
docs/features/market-events/cards/CARD-030-document-dashboard-market-event-template-update-contract.md
```

## Constraints

- Do not document speculative routes or fields not implemented by `CARD-029`.
- Do not add template delete behavior.
- Do not redefine pressure-ladder pricing, event scheduler, quote, execute, drift, blocking, or lifecycle behavior.
- Keep this card documentation-only.

## Validation Commands

```bash
rg -n "event-templates|template update|MarketEventTemplate|SCOPE_market:admin|delete" docs/features/market-events/contract.md docs/features/market-events/cards/CARD-030-document-dashboard-market-event-template-update-contract.md
```

Fallback if `rg` is unavailable:

```bash
grep -nE "event-templates|template update|MarketEventTemplate|SCOPE_market:admin|delete" docs/features/market-events/contract.md docs/features/market-events/cards/CARD-030-document-dashboard-market-event-template-update-contract.md
```

## Out of Scope

- Backend route, DTO, service, validation, persistence, or security implementation.
- Dashboard frontend client, table, or modal behavior.
- Market event template deletion.
- Public market API changes.

## Completion Notes

- Documented `PUT /api/dashboard/market/event-templates/{templateId}` in the
  market-events feature contract.
- Recorded the complete update request shape, response row shape,
  path-bound immutable `templateId` behavior, `SCOPE_market:admin` boundary,
  validation/error ownership, delete out-of-scope status, and downstream
  dashboard ownership limits.
- Validation: `rtk rg -n
  "event-templates|template update|MarketEventTemplate|SCOPE_market:admin|delete"
  docs/features/market-events/contract.md
  docs/features/market-events/cards/CARD-030-document-dashboard-market-event-template-update-contract.md`
  passed from the repository root.
