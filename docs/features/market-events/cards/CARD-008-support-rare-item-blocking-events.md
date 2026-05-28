# CARD-008: Support Rare Item Blocking Events

## Status

planned

## Objective

Support narrow rare/manual item blocking events that reject trades while active.

## Context

Blocking is MVP-supported only for rare/manual events. For MVP, a blocking event should usually just block, not combine with price effects.

## Required Reading

- `../contract.md`
- `../../../market-contract-mvp.md`

## Expected Behavior

An active blocking event marks affected items as blocked for snapshots and rejects quote or execute attempts according to existing blocked item semantics. Issued quotes are not proactively cancelled, but execution rejects if the item becomes blocked before execution.

## Acceptance Criteria

- [ ] Blocking templates are rare/manual-only unless explicitly enabled for safe automatic rare templates.
- [ ] Blocking applies at item level for MVP.
- [ ] Blocked items appear blocked in snapshots.
- [ ] Quote requests for blocked items reject using the existing blocked/non-tradable rejection semantics.
- [ ] Execute rejects if an item becomes blocked after quote creation and before execution.
- [ ] Blocking events do not mutate player assets, market pressure, or balances.
- [ ] Blocking events do not automatically shut down the full market.

## Expected Files to Change

```text
docs/features/market-events/contract.md
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/
```

## Constraints

- Do not bundle price effects into automatic blocking events for MVP.
- Do not add player-specific blocking.
- Do not cancel outstanding quotes proactively.
- Do not reinterpret `operating`.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradePlannerTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketTradeExecutorTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
```

Run from `java/`.

## Out of Scope

- Category-wide blocking.
- Full market shutdown.
- Client UI treatment.

## Completion Notes
