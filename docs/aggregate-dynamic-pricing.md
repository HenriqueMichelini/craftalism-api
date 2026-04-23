# Market Aggregate — Dynamic Pricing System

## 1. Overview

This system implements a dynamic, segment-based market where:

- pricing is driven by segmented supply layers
- execution is stateful and momentum-driven
- regeneration restores the system toward equilibrium
- all behavior is deterministic

This is NOT a stock-first system.

The authoritative execution state is defined by **segment state**, not by `currentStock`.

---

## 2. Aggregate Design

### Aggregate Root

`MarketItem`

Responsibilities:
- entry point for all mutations
- persistence boundary
- projection holder (derived values)

### Authoritative State

`MarketSegmentState[]`

Each segment represents a **price layer with capacity**.

Fields:

- `itemId`
- `segmentIndex` (0-based)
- `maxCapacity`
- `remainingCapacity`
- `unitPrice`

👉 This is the **ONLY mutable execution state**

---

## 3. Derived Projections (Non-Authoritative)

Stored on `MarketItem` for convenience:

- `currentStock = sum(remainingCapacity)`
- `marketMomentum`
- `buyUnitEstimate`
- `sellUnitEstimate`

Rules:
- NEVER mutated directly
- ALWAYS derived from segment state

---

## 4. Core Concepts

### 4.1 Segment Model

Segments are ordered:
0 → 1 → 2 → ... → n

- Lower index = cheaper
- Higher index = more expensive

---

### 4.2 Frontier Definitions

- **buyFrontier**
  - lowest index where `remainingCapacity > 0`

- **restoreFrontier**
  - highest index where `remainingCapacity < maxCapacity`

---

### 4.3 Momentum

Definition:

marketMomentum = restoreFrontier
marketMomentum = -1 if fully restored

Meaning:

- `-1` → fully restored
- `0` → first segment active
- `k` → currently consuming/restoring segment k

---

## 5. Core Invariant

There must be **at most one partially consumed segment**.

Valid state:
[ fully consumed ][ partially consumed ][ untouched ]

Invalid examples:
- gaps between segments
- multiple partial segments
- lower segment untouched while higher is consumed

---

## 6. Buy Transition

### Behavior

- traverse segments from `buyFrontier`
- consume capacity progressively

### Algorithm

1. Lock item + segments
2. Find `buyFrontier`
3. For each segment:
   - `take = min(requestRemaining, remainingCapacity)`
   - decrease `remainingCapacity`
   - accumulate price
4. Stop when:
   - request is fulfilled
   - or no capacity remains
5. If full-fill required and not satisfied → reject
6. Recompute projections
7. Persist

### Rules

- NO direct `currentStock -= quantity`
- execution is strictly segment-based

---

## 7. Sell Transition

### Behavior

- inverse of buy
- restores capacity in reverse order

### Algorithm

1. Lock item + segments
2. Find `restoreFrontier`
3. Traverse backward
4. For each segment:
   - `restorable = maxCapacity - remainingCapacity`
   - `take = min(requestRemaining, restorable)`
   - increase `remainingCapacity`
5. Stop when fulfilled
6. Recompute projections
7. Persist

---

## 8. Regeneration

### Behavior

- same as sell, but automated

### Algorithm

1. Compute deterministic regen quantity
2. Lock segments
3. Start from `restoreFrontier`
4. Restore capacity progressively
5. Stop when regen exhausted or fully restored
6. Recompute projections
7. Persist

---

## 9. Momentum Rules

- momentum increases when:
  - buy crosses a segment boundary

- momentum decreases when:
  - sell/regeneration fully restores a segment

- momentum remains unchanged when:
  - operating inside a segment

---

## 10. Hard Invariants

### Segment-level

- `0 <= remainingCapacity <= maxCapacity`
- `maxCapacity > 0`
- `unitPrice > 0`

### Aggregate-level

- segment indexes are contiguous: `0..n-1`
- at most one partially consumed segment
- no gaps in consumption order
- `currentStock == sum(remainingCapacity)`
- `marketMomentum == restoreFrontier or -1`

### Execution-level

- executed quantity equals sum of segment deltas
- no operation may create negative capacity
- no partial mutation on failed operations

---

## 11. Migration Strategy

### Problem

Legacy model is lossy:
- `currentStock`
- `marketMomentum`
(no segment state)

### Backfill Algorithm

1. Generate canonical segments
2. Read legacy momentum
3. If `momentum = -1`:
   - all segments fully restored
4. Else:
   - segments `< momentum` → fully consumed
   - segment `momentum` → partially consumed
   - segments `> momentum` → untouched
5. Adjust frontier segment to match `currentStock`
6. Recompute projections

### Failure Handling

- if mismatch occurs → fail migration or mark for repair
- DO NOT silently fix inconsistencies

---

## 12. Testing Strategy

### Unit

- stock derivation
- momentum derivation
- frontier detection
- invalid state rejection

### Buy

- single segment
- multi-segment
- boundary crossing
- insufficient stock
- regression: 2304 case

### Sell

- partial restore
- full restore
- multi-boundary restore

### Regeneration

- partial restore
- boundary crossing
- full restore → momentum = -1

### Integration

- quote → execute consistency
- projection correctness

---

## 13. Design Principles

- segment-first execution
- deterministic behavior
- no hidden state
- single source of truth
- invariant-driven design

---

## 14. Implementation Decisions

- Segment definitions are persisted per item.
- Segment rows store maxCapacity, remainingCapacity, and unitPrice.
- Buy and sell use full-fill semantics for now.
- Sell cannot restore beyond total consumed capacity.

## 15. Summary

This system is defined by:

- segment-based execution state
- frontier-based momentum
- progressive pricing
- symmetric buy/sell/regen logic

The aggregate must guarantee:

👉 all invariants are valid after every operation
