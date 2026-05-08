# CARD-018: Extract Market Snapshot Orchestration

## Status

completed

## Objective

Move snapshot projection and snapshot-version orchestration out of `MarketService`.

## Context

RefactorFirst identified `MarketService.java` as both a God Class and a highly coupled class. Snapshot reads, projection timing, response assembly, and current snapshot version calculation are separable from quote and execute orchestration.

This is a behavior-preserving refactor. `craftalism-api` owns snapshot projection, snapshot version semantics, and pressure-ladder read behavior.

## Required Reading

- `../contract.md`
- `../../../market-pressure-ladder-sigmoid-pricing.md`

## Expected Behavior

Snapshot responses and snapshot version values remain unchanged while `MarketService` delegates snapshot read and projection work to a focused collaborator.

## Acceptance Criteria

- [ ] `MarketService.getSnapshot()` delegates snapshot orchestration to a focused component.
- [ ] Quote and execute paths use the same component for current snapshot version calculation.
- [ ] Snapshot timing logs preserve the same meaningful timing fields or move to the focused component without losing observability.
- [ ] Existing snapshot version hashing and projection behavior remain unchanged.
- [ ] Existing snapshot, quote, and execute tests pass without expectation changes.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotService.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjectorTest.java
```

## Constraints

- Do not change snapshot DTO fields.
- Do not change snapshot version semantics.
- Do not change pressure pricing or regeneration rules.
- Do not change public endpoints.
- Do not introduce unrelated refactors.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.service.MarketServiceTest --tests io.github.HenriqueMichelini.craftalism.api.service.MarketSnapshotProjectorTest
```

Run from `java/`.

Fallback before completion:

```bash
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Quote lifecycle changes.
- Execute settlement changes.
- DTO or API contract changes.
- Persistence changes.

## Suggested Commit Message

`refactor(craftalism-api): extract market snapshot orchestration`

## Completion Notes

- Extracted snapshot read/projection/version orchestration from `MarketService` into `MarketSnapshotService`.
- `MarketService.getSnapshot()`, quote snapshot reads, execute snapshot-version checks, and execute updated-item projection now delegate through the focused snapshot component.
- Preserved existing snapshot timing fields, snapshot DTO projection, and snapshot-version hashing behavior.
