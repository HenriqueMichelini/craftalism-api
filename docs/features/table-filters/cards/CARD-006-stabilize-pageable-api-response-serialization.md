# CARD-006: Stabilize Pageable API Response Serialization

## Status

completed

## Objective

Replace direct Spring Data `PageImpl` HTTP serialization with an API-owned stable page response DTO for transaction and market trade-history list reads.

## Context

`GET /api/transactions` and `GET /api/market/trades` currently return Spring Data
`Page<T>` values directly from controllers. Spring Data logs a warning because
serializing `PageImpl` as-is does not guarantee a stable JSON structure.

The dashboard already consumes the existing pageable response envelope. This
card makes that envelope explicit and backend-owned without migrating consumers
to Spring Data `PagedModel` or changing pagination semantics.

## Required Reading

- `../contract.md`
- `../../market-trade-history/contract.md`

## Expected Behavior

`GET /api/transactions` and `GET /api/market/trades` return an explicit,
API-owned page response DTO with the same HTTP JSON envelope and pagination
metadata currently consumed by clients. Serializing those responses no longer
emits Spring Data's unsupported `PageImpl` serialization warning.

## Acceptance Criteria

- [ ] An API-owned generic page response DTO represents paged HTTP responses without exposing Spring Data implementation types to Jackson.
- [ ] `GET /api/transactions` maps its service `Page<TransactionResponseDTO>` result to the stable page response DTO.
- [ ] `GET /api/market/trades` maps its service `Page<MarketTradeHistoryDTO>` result to the stable page response DTO.
- [ ] The HTTP JSON envelope preserves the currently exposed `content`, pagination metadata, sort metadata, and empty-page behavior expected by existing consumers.
- [ ] Pagination query parameters, filter behavior, sorting behavior, security behavior, and service return types remain unchanged.
- [ ] Controller or integration tests assert representative populated and empty stable page response shapes for both endpoints.
- [ ] The table-filter and market-trade-history contracts describe the explicit API-owned stable page response envelope instead of relying on direct Spring `Page<T>` serialization.
- [ ] HTTP serialization of both paged endpoints no longer emits Spring Data's unsupported `PageImpl` serialization warning.

## Expected Files to Change

```text
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/dto/PageResponseDTO.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionController.java
java/src/main/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketController.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/TransactionControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketControllerTest.java
java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java
docs/features/table-filters/contract.md
docs/features/market-trade-history/contract.md
```

Add or update the nearest transaction HTTP integration test if controller tests
alone cannot verify the serialized transaction response shape.

## Constraints

- Preserve the existing client-visible JSON envelope; this card stabilizes the public contract rather than redesigning it.
- Do not enable `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` because Spring Data `PagedModel` nests pagination metadata under `page` and would require a coordinated consumer migration.
- Do not change repository, service, filter, sorting, or authorization behavior.
- Do not change non-pageable endpoints.
- Do not modify dashboard code or shared-root contracts from this repository.

## Validation Commands

```bash
rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest
rtk ./gradlew test
```

Run from `java/`.

## Out of Scope

- Migrating API consumers to Spring Data `PagedModel`.
- Adding hypermedia links or Spring HATEOAS.
- Adding new filters, sort properties, or pagination parameters.
- Changing dashboard code.
- Publishing shared-root contract changes.

## Suggested Commit Message

`fix(craftalism-api): stabilize pageable API response serialization`

## Completion Notes

- Added API-owned `PageResponseDTO<T>` with explicit pageable and sort metadata.
- Mapped transaction and market trade-history list responses at controller boundaries.
- Added populated and empty HTTP envelope coverage for both paged endpoints.
- Updated repo-local contracts to describe the stable API-owned response envelope.
- Validation passed:
  - `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketControllerTest --tests io.github.HenriqueMichelini.craftalism.api.controller.MarketContractIntegrationTest --tests io.github.HenriqueMichelini.craftalism.api.controller.TransactionContractIntegrationTest`
  - `rtk ./gradlew test --tests io.github.HenriqueMichelini.craftalism.api.security.SecurityFilterChainTest`
  - `rtk ./gradlew test`
