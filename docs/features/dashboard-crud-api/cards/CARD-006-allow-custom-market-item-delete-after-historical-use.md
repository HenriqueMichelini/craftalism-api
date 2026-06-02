# CARD-006: Allow Custom Market Item Delete After Historical Use

## Status

planned

## Objective

Allow dashboard administrators to delete non-default market items after historical use while preserving quote and trade-history records and rejecting deletion while an active quote still references the item.

## Context

Market quotes and trade history store `itemId` as historical text values without foreign keys to `market_items`. The current dashboard delete path rejects any quote or trade-history reference, which prevents custom market items and their categories from being removed after ordinary market use.

Active quotes still require the market item during execution, so deletion must remain blocked while any `ACTIVE` quote references the item.

## Required Reading

- `../contract.md`

## Expected Behavior

`DELETE /api/dashboard/market/items/{itemId}` deletes a non-default market item when no active quote references it. Historical trade records and resolved quote records remain intact. Default catalog items and items referenced by active quotes still return `409 ProblemDetail`.

After all items in a custom category are deleted, the existing category delete route can delete that category.

## Acceptance Criteria

- [ ] Deleting a non-default market item referenced only by trade history returns `204`.
- [ ] Deleting a non-default market item referenced only by non-active quotes returns `204`.
- [ ] Quote and trade-history records are preserved after the market item is deleted.
- [ ] Deleting a non-default market item referenced by an `ACTIVE` quote returns `409 ProblemDetail`.
- [ ] Deleting a default catalog item continues to return `409 ProblemDetail`.
- [ ] Deleting the final item in a custom category allows the existing category delete route to delete that category.
- [ ] The dashboard CRUD API contract documents the narrowed market item delete restriction.

## Expected Files to Change

```text
docs/features/dashboard-crud-api/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/market/application/admin/DashboardMarketItemService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketQuoteRepository.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketTradeHistoryRepository.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketItemCrudApiIntegrationTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/DashboardMarketCategoryCrudApiIntegrationTest.java
```

## Constraints

- Do not delete or mutate quote or trade-history records when deleting a market item.
- Do not allow deletion while an `ACTIVE` quote references the item.
- Do not allow dashboard deletion of default catalog items.
- Do not change public market snapshot, quote, execute, or trade-history route behavior.
- Do not change persistence schema.
- Do not modify unrelated features.

## Validation Commands

```bash
./gradlew test --tests '*DashboardMarketItemCrudApiIntegrationTest' --tests '*DashboardMarketCategoryCrudApiIntegrationTest'
```

Fallback if the filtered command is unavailable:

```bash
./gradlew test
```

## Out of Scope

- Archiving or soft deletion.
- Deleting default catalog items.
- Deleting, rewriting, or expiring active quotes as part of item deletion.
- Deleting or rewriting quote and trade-history records.
- Changing market event target metadata lifecycle behavior.
- Persistence schema changes.
- Dashboard frontend changes.

## Completion Notes

