# Market Pressure-Ladder Implementation Cards

Source converted: confirmed repo-local audit findings from the pressure-ladder audit.

Target source of truth: `docs/market-pressure-ladder-sigmoid-pricing.md`.

Historical context only: `docs/aggregate-dynamic-pricing.md`.

Repository ownership: `craftalism-api` owns authoritative backend market state, persistence, pricing, quote planning, execution, regeneration, snapshot DTOs, `snapshotVersion`, rejection semantics, catalog defaults, validation, and backend tests.

Out-of-repo boundary: `craftalism-market` consumes the pressure-ladder contract and must be updated outside this repository after the backend contract changes.

## CARD-001 - Add pressure schema migration

### Objective
Add pressure-ladder columns to `market_items` and deterministic legacy backfill from `market_segments`.

### Source
Audit findings 1, 2, and 3.

### Repository Ownership
`craftalism-api` owns durable market state and Flyway migrations.

### Responsibilities Involved
Persistence correctness, market authoritative state, migration safety.

### Consumed Contracts
Existing database state from legacy `market_items` and `market_segments`.

### Files Likely to Read
- `java/src/main/resources/db/migration/V8__create_market_items_table.sql`
- `java/src/main/resources/db/migration/V12__create_market_segments_table.sql`
- `java/src/main/resources/db/migration/V13__backfill_market_segments_from_legacy_state.sql`
- `docs/market-pressure-ladder-sigmoid-pricing.md`

### Files Likely to Change
- `java/src/main/resources/db/migration/V15__*.sql`
- Migration test location if available

### Acceptance Criteria
- `market_items` has `base_unit_price`, `min_unit_price`, `max_unit_price`, `segment_size`, `price_sensitivity`, `base_regen_quantity`, `regen_interval_seconds`, `net_position`, `min_net_position`, and `max_net_position`.
- `net_position` is backfilled as `sum(max_capacity - remaining_capacity)`.
- Backfill checks `current_stock == sum(remaining_capacity)` and fails or clearly flags inconsistent legacy state.

### Validation
- `rtk ./gradlew test` from `java/`
- Any Flyway or migration-specific test available

### Non-Goals
- Do not switch runtime code to pressure planning.
- Do not remove `market_segments`.

### Risk Notes
Highest operational risk is bad legacy data. Keep migration deterministic and auditable.

### Suggested Commit Message
`feat(craftalism-api): add pressure market state migration`

## CARD-002 - Map pressure fields on MarketItem

### Objective
Update `MarketItem` to represent pressure-ladder authoritative state.

### Source
Audit finding 1.

### Repository Ownership
`craftalism-api` owns the market aggregate root.

### Responsibilities Involved
Domain model, persistence mapping, aggregate invariants.

### Consumed Contracts
Database columns from CARD-001.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketItem.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketSegment.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketItemRepository.java`

### Files Likely to Change
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketItem.java`

### Acceptance Criteria
- `MarketItem` exposes all required pressure config and state fields.
- Legacy segment relationship remains only if still needed for migration compatibility.
- No behavior changes are made outside model mapping.

### Validation
- Compile and focused model/repository tests

### Non-Goals
- Do not rewrite planner.
- Do not change public DTOs yet.

### Risk Notes
JPA mapping must remain compatible with existing migrations and tests.

### Suggested Commit Message
`feat(craftalism-api): map pressure fields on market items`

## CARD-003 - Add pressure catalog defaults and validation

### Objective
Replace segment-count catalog configuration with pressure-pricing defaults and hard validation.

### Source
Audit finding 10.

### Repository Ownership
`craftalism-api` owns backend market item configuration.

### Responsibilities Involved
Catalog seeding, item validation, configuration defaults.

### Consumed Contracts
Pressure-ladder required defaults and validation rules.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DefaultMarketCatalog.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItem.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializer.java`

### Files Likely to Change
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/DefaultMarketCatalog.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSeedItem.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializer.java`
- Related catalog tests

### Acceptance Criteria
- Catalog items define `baseUnitPrice`, `minUnitPrice`, `maxUnitPrice`, `segmentSize`, `priceSensitivity`, `baseRegenQuantity`, `regenIntervalSeconds`, and optional bounds.
- Validation enforces all hard rules from the design doc.
- New seed items initialize `netPosition = 0`.

### Validation
- Catalog initializer unit tests

### Non-Goals
- Do not remove legacy segment migration code.
- Do not change quote or execute behavior yet.

### Risk Notes
Bad defaults affect all item pricing. Use documented initial defaults unless explicit overrides are required.

### Suggested Commit Message
`feat(craftalism-api): seed pressure market catalog defaults`

## CARD-004 - Implement pressure price derivation

### Objective
Introduce deterministic pressure segment and unit price derivation.

### Source
Audit finding 4.

### Repository Ownership
`craftalism-api` owns authoritative market pricing.

### Responsibilities Involved
Segment derivation, anchored bounded price curve, projection math.

### Consumed Contracts
Pressure-ladder pricing formula.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java`

### Files Likely to Change
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java`
- Or a new package-private pricing helper
- Focused unit tests

### Acceptance Criteria
- Uses `Math.floorDiv(netPosition, segmentSize)` for segment derivation.
- Segment `0` prices exactly at `baseUnitPrice`.
- Positive pressure approaches `maxUnitPrice`; negative pressure approaches `minUnitPrice`.
- Unit price is rounded and clamped within bounds.

### Validation
- Unit tests for positive, zero, negative, and boundary positions

### Non-Goals
- Do not wire quote planning yet unless necessary.
- Do not change DTOs.

### Risk Notes
Negative floor-division behavior is easy to get wrong and must be explicitly tested.

### Suggested Commit Message
`feat(craftalism-api): derive pressure ladder prices`

## CARD-005 - Replace quote planning traversal

### Objective
Make quote planning walk virtual pressure positions instead of persisted segments.

### Source
Audit findings 4 and 9.

### Repository Ownership
`craftalism-api` owns quote totals and rejection semantics.

### Responsibilities Involved
Quote planning, overflow checks, hard pressure bounds, effective unit price.

### Consumed Contracts
Quote request and response shape remains unchanged.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/exceptions/MarketRejectionCode.java`

### Files Likely to Change
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java`
- Planner tests
- Possibly service tests

### Acceptance Criteria
- BUY prices positions `netPosition` through `netPosition + quantity - 1`.
- SELL prices positions `netPosition - 1` through `netPosition - quantity`.
- `INSUFFICIENT_STOCK` is emitted only for configured pressure bounds.
- Ordinary buys and sells are not limited by finite stock.

### Validation
- Planner unit tests for crossing segment boundaries in both directions, bounds, and overflow

### Non-Goals
- Do not mutate `netPosition` in execute yet.
- Do not update snapshot contract yet.

### Risk Notes
This changes quote totals and will invalidate many legacy tests.

### Suggested Commit Message
`feat(craftalism-api): plan quotes over pressure positions`

## CARD-006 - Mutate netPosition during execute

### Objective
Update execute settlement to mutate `netPosition` after quote verification and successful settlement.

### Source
Audit finding 5.

### Repository Ownership
`craftalism-api` owns trade execution and market mutation.

### Responsibilities Involved
Quote-backed execute, balance settlement, single-use quote lifecycle, failed-settlement safety.

### Consumed Contracts
Existing execute endpoint and quote-token contract.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketQuoteStore.java`
- Execute tests

### Files Likely to Change
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutor.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java`
- Execute unit and integration tests

### Acceptance Criteria
- BUY increments `netPosition` by quantity.
- SELL decrements `netPosition` by quantity.
- Failed settlement does not mutate pressure.
- Rebuilt plan must match stored quote total and unit price before mutation.

### Validation
- Execute tests for buy, sell, insufficient funds, stale quote, and single-use quote

### Non-Goals
- Do not alter public snapshot fields in this card.
- Do not remove legacy segment entities yet.

### Risk Notes
Settlement order matters: preserve quote single-use semantics and no market mutation on failed settlement.

### Suggested Commit Message
`feat(craftalism-api): execute trades against market pressure`

## CARD-007 - Implement pressure regeneration

### Objective
Replace stock restoration regeneration with deterministic pressure recovery toward zero.

### Source
Audit finding 6.

### Repository Ownership
`craftalism-api` owns regeneration and stale detection state.

### Responsibilities Involved
Regeneration, `lastUpdatedAt`, derived projections, persistence.

### Consumed Contracts
Per-item regen config from pressure model.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketReadService.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlanner.java`

### Files Likely to Change
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketReadService.java`
- Regeneration tests

### Acceptance Criteria
- Positive `netPosition` decreases toward `0`.
- Negative `netPosition` increases toward `0`.
- `lastUpdatedAt` advances only by whole applied ticks.
- Fractional tick remainder is preserved.

### Validation
- Unit and integration tests for positive, negative, zero pressure, no tick, and multiple ticks

### Non-Goals
- Do not change quote or execute endpoint shapes.
- Do not implement client behavior.

### Risk Notes
Changing `lastUpdatedAt` behavior affects `snapshotVersion`, stale quote detection, and repeated snapshot reads.

### Suggested Commit Message
`feat(craftalism-api): regenerate market pressure toward equilibrium`

## CARD-008 - Update snapshot DTO and projector

### Objective
Expose pressure snapshot fields and remove target-contract `currentStock`.

### Source
Audit finding 7.

### Repository Ownership
`craftalism-api` owns the canonical market snapshot contract.

### Responsibilities Involved
Snapshot DTO, projection, execute response `updatedItem`.

### Consumed Contracts
`craftalism-market` consumes this API shape.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketSnapshotItemDTO.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/MarketExecuteSuccessResponseDTO.java`
- `docs/market-contract-mvp.md`

### Files Likely to Change
- DTOs
- Projector
- Controller contract tests

### Acceptance Criteria
- Snapshot item exposes `marketPressure`, `marketSegment`, and `pressureMagnitude`.
- Target snapshot no longer exposes `currentStock`.
- Execute success `updatedItem` uses the same pressure item shape.

### Validation
- Controller integration tests for snapshot and execute response

### Non-Goals
- Do not add a client compatibility adapter unless explicitly requested.
- Do not change quote request or response shape.

### Risk Notes
Breaking API contract for old clients; this is expected by the pressure-ladder design.

### Suggested Commit Message
`feat(craftalism-api): expose pressure market snapshots`

## CARD-009 - Rebuild snapshotVersion hashing

### Objective
Hash only authoritative pressure state and trade-affecting config.

### Source
Audit finding 8.

### Repository Ownership
`craftalism-api` owns stale detection semantics.

### Responsibilities Involved
Snapshot version, quote stale checks, regeneration boundary.

### Consumed Contracts
Clients treat `snapshotVersion` as opaque.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java`

### Files Likely to Change
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketSnapshotProjector.java`
- Snapshot version tests

### Acceptance Criteria
- Hash includes required pressure config, `netPosition`, bounds, blocked/operating, and deterministic regen boundary.
- Hash does not include persisted derived projections.
- Hash does not include segment rows or virtual segment lists.

### Validation
- Unit tests proving version changes for authoritative state/config changes and stays stable for derived-only recalculation

### Non-Goals
- Do not expose hash internals to clients.
- Do not make token parseable.

### Risk Notes
Snapshot version changes directly affect quote stale rejection behavior.

### Suggested Commit Message
`feat(craftalism-api): hash pressure market snapshot state`

## CARD-010 - Remove runtime segment dependency

### Objective
Stop normal market runtime reads, planning, execution, and projections from depending on `market_segments`.

### Source
Audit findings 2 and 3.

### Repository Ownership
`craftalism-api` owns normal backend market operation.

### Responsibilities Involved
Repository queries, runtime persistence boundaries, legacy migration boundary.

### Consumed Contracts
Legacy segments may remain as migration or audit data only.

### Files Likely to Read
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketItemRepository.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketSegmentRepository.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketSegment.java`
- `java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketCatalogInitializer.java`

### Files Likely to Change
- Repositories
- Services still fetching segments
- Bootstrap and performance tests

### Acceptance Criteria
- Normal snapshot, quote, and execute paths do not fetch `m.segments`.
- `market_segments` is not required for new catalog runtime behavior.
- Any remaining segment usage is explicitly migration or audit only.

### Validation
- Integration and performance tests confirm bounded read behavior without segment fetch fan-out

### Non-Goals
- Do not drop the `market_segments` table yet unless the pressure path has already been verified.
- Do not alter historical migrations.

### Risk Notes
Removing fetch joins may expose hidden dependencies in tests and bootstrap code.

### Suggested Commit Message
`refactor(craftalism-api): remove runtime market segment dependency`

## CARD-011 - Replace legacy contract and integration tests

### Objective
Align unit, integration, and contract tests with pressure-ladder behavior.

### Source
Audit finding 11.

### Repository Ownership
`craftalism-api` owns backend test confidence for market contracts.

### Responsibilities Involved
Testing, contract verification, migration confidence.

### Consumed Contracts
Pressure-ladder doc and market contract doc.

### Files Likely to Read
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradePlannerTest.java`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketReadServiceTest.java`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketTradeExecutorTest.java`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/service/MarketServiceTest.java`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java`
- `java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketBootstrapPerformanceIntegrationTest.java`

### Files Likely to Change
- Market unit and integration tests
- Migration tests if present

### Acceptance Criteria
- Tests cover segment derivation for positive, zero, and negative pressure.
- Tests cover quote traversal across positive and negative virtual segments.
- Tests cover snapshot pressure fields and absence of target `currentStock`.
- Tests cover hard-bound `INSUFFICIENT_STOCK`, stale quote, single-use quote, and failed settlement no mutation.
- Tests cover migration and backfill consistency.

### Validation
- `rtk ./gradlew test`

### Non-Goals
- Do not preserve tests that assert superseded stock semantics.
- Do not add client-side tests in this repository.

### Risk Notes
Best done after behavior cards, or incrementally alongside each behavior card if keeping the suite green per commit.

### Suggested Commit Message
`test(craftalism-api): cover pressure ladder market behavior`

## CARD-012 - Update API docs for implemented pressure contract

### Objective
Align repo-local public docs after implementation matches the pressure-ladder contract.

### Source
Audit findings 7, 8, 9, and 11.

### Repository Ownership
`craftalism-api` owns canonical backend market API docs.

### Responsibilities Involved
Documentation alignment, consumer contract clarity.

### Consumed Contracts
`craftalism-market` consumes documented API behavior.

### Files Likely to Read
- `README.md`
- `docs/market-contract-mvp.md`
- `docs/craftalism-market-pressure-ladder-changelog.md`

### Files Likely to Change
- `README.md`
- Repo-local market contract docs only if drift remains after implementation

### Acceptance Criteria
- Docs show pressure fields, not target `currentStock`.
- `INSUFFICIENT_STOCK` docs mention only configured hard pressure bounds.
- Snapshot version docs match authoritative pressure hash semantics.

### Validation
- Manual doc review
- Contract tests

### Non-Goals
- Do not document behavior before it is implemented.
- Do not edit client repository docs.

### Risk Notes
Docs should trail code behavior, not lead it, unless a card explicitly changes only docs.

### Suggested Commit Message
`docs(craftalism-api): align market docs with pressure contract`

## Safest First Card

Start with CARD-001 - Add pressure schema migration.

It is additive, keeps the legacy runtime path intact, and gives later cards a stable persistence foundation while preserving rollback and audit visibility.
