# Market Next Implementations

Date: 2026-04-12

## Ownership

`craftalism-api` owns the authoritative market contract:

- snapshot payloads
- quote payloads
- execute semantics
- quote lifecycle semantics
- rejection codes and response shape

`craftalism-market` is a consumer and must not redefine these behaviors locally.

## Current State

Implemented in this repository:

- `GET /api/market/snapshot`
- `POST /api/market/quotes`
- `POST /api/market/execute`
- persistent market item storage
- persistent market quote storage
- machine-readable market rejection payloads
- quote lifecycle states:
  - `ACTIVE`
  - `CONSUMED`
  - `EXPIRED`
  - `INVALIDATED`
- single-use quote consumption

Relevant files:

- [`docs/market-contract-mvp.md`](/home/henriquemichelini/IdeaProjects/craftalism-api/docs/market-contract-mvp.md)
- [`java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java`](/home/henriquemichelini/IdeaProjects/craftalism-api/java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java)
- [`java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketQuoteStore.java`](/home/henriquemichelini/IdeaProjects/craftalism-api/java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketQuoteStore.java)
- [`java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketQuote.java`](/home/henriquemichelini/IdeaProjects/craftalism-api/java/src/main/java/io/github/HenriqueMichelini/craftalism/api/model/MarketQuote.java)
- [`java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketQuoteRepository.java`](/home/henriquemichelini/IdeaProjects/craftalism-api/java/src/main/java/io/github/HenriqueMichelini/craftalism/api/repository/MarketQuoteRepository.java)
- [`java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java`](/home/henriquemichelini/IdeaProjects/craftalism-api/java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java)

## Important Contract Decision Already Implemented

Execute now has single-use quote semantics.

Meaning:

- the first execute attempt claims the quote
- later replay attempts reject with `STALE_QUOTE`
- if a quote is stale or mismatched, it may become `INVALIDATED`
- if a quote is expired, it may become `EXPIRED`

Important consequence:

- a quote can become `CONSUMED` even when settlement later rejects for a business rule such as `INSUFFICIENT_FUNDS`

This is currently the implemented behavior and should be treated as authoritative unless intentionally changed.

## Highest-Value Next Implementation

### 1. Add True Concurrent Execute Coverage

Current tests cover replay, but not true concurrent execution against the same `quoteToken`.

Add an integration test that:

- creates one quote
- sends two execute requests concurrently for that same quote
- verifies exactly one request can succeed or claim the quote
- verifies the other request rejects deterministically
- verifies final quote state is correct
- verifies balance and stock move at most once

Why this matters:

- this is the real proof that single-use semantics hold under contention
- replay tests alone are weaker than true concurrent tests

Likely file:

- [`java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java`](/home/henriquemichelini/IdeaProjects/craftalism-api/java/src/test/java/io/github/HenriqueMichelini/craftalism/api/controller/MarketContractIntegrationTest.java)

## Next Contract Clarification

### 2. Update the Spec Doc to Match Single-Use Execute Semantics

The contract doc should explicitly say:

- execute is single-use for a quote
- the first execute attempt spends the quote
- retries with the same `quoteToken` are not guaranteed to re-run settlement
- a quote may be consumed even if settlement later returns a business rejection

Likely file:

- [`docs/market-contract-mvp.md`](/home/henriquemichelini/IdeaProjects/craftalism-api/docs/market-contract-mvp.md)

This avoids consumer-side guesswork in `craftalism-market`.

## Next Design Decision To Resolve

### 3. Decide Whether Consuming Before Final Settlement Is the Intended Long-Term Rule

Current implementation prioritizes unambiguous single-use behavior.

This is good for:

- replay safety
- deterministic execute semantics
- avoiding ambiguous quote reuse

But it also means:

- `INSUFFICIENT_FUNDS` after quote claim leaves the quote spent

That may be acceptable, but it should be an explicit contract choice.

Options:

1. Keep current behavior.
   Best for strict single-use semantics.

2. Pre-check more conditions before consumption.
   Reduces spent-on-rejection cases, but weakens the clean atomic claim boundary.

3. Introduce a separate reserved or processing state.
   Stronger model, but more complexity than current MVP likely needs.

Recommended default:

- keep current behavior unless consumer or product requirements clearly reject it

## Later Repo-Local Improvements

### 4. Replace Seeded Catalog Logic

Current market item bootstrap is still MVP-grade.

Future work:

- move seed logic out of service code
- add explicit catalog administration or authoritative data loading

Relevant file:

- [`java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java`](/home/henriquemichelini/IdeaProjects/craftalism-api/java/src/main/java/io/github/HenriqueMichelini/craftalism/api/service/MarketService.java)

### 5. Decide Sell-Side Boundary

Current repo settles sell-side economy effects, but does not validate player inventory ownership.

Next session should explicitly confirm:

- whether inventory validation belongs in this repo
- or whether that boundary is owned elsewhere and this repo should stop at economic settlement

Do not implement cross-repo inventory ownership checks locally unless ownership is confirmed.

## Validation Baseline

At handoff time:

- `cd java && ./gradlew test` passes

## Suggested Next Session Order

1. Update the contract doc for single-use execute semantics.
2. Add a concurrent execute integration test.
3. Only if that test exposes a real bug, change execute/quote lifecycle code.
4. Re-run `./gradlew test`.

