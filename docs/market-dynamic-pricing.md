# Market Dynamic Pricing System

## Overview

This system implements a dynamic game economy where prices react to player actions and stock conditions.

Unlike fixed-price systems, this model:
- reacts to supply and demand
- applies progressive pricing within a single transaction
- evolves over time through regeneration
- uses momentum as a stateful representation of market pressure

Dynamic pricing systems adjust prices based on demand, supply, and system state rather than remaining constant :contentReference[oaicite:0]{index=0}.

---

## Core Concepts

### 1. Momentum (State)

Momentum is the **primary state variable** of the market.

- It defines the current pricing structure
- It determines which price segments are active
- It influences regeneration behavior

Momentum is NOT a modifier — it is the **structure of the market**

---

### 2. Segments (Price Layers)

The market is divided into ordered segments.

Each segment contains:
- `capacity` (how many items exist in this segment)
- `unit_price`

Example:

| Segment | Capacity | Price |
|--------|--------|------|
| 1 | 1000 | 1.0 |
| 2 | 800 | 1.5 |
| 3 | 2000 | 5.56 |

Segments are consumed sequentially.

---

### 3. Momentum → Segment Mapping

Momentum determines which segments are active.

- Higher momentum → deeper into higher-priced segments
- Lower momentum → closer to base segments

Interpretation:

- Momentum = index/state of progression across segments
- Each increment in momentum corresponds to advancing into a higher price layer

---

## Buy Operation (Quote Flow)

### Rule: Progressive Consumption

A buy operation MUST:

1. Start from the current segment
2. Consume available capacity
3. Move to the next segment if needed
4. Continue until quantity is fulfilled

### Algorithm
remaining = requested_quantity
total_price = 0

for each segment in order:
if remaining == 0:
break

take = min(segment.remaining_capacity, remaining)

total_price += take * segment.unit_price
remaining -= take

consume segment capacity

return total_price


### Properties

- price is NOT constant
- large orders increase average price
- small orders stay near current segment price

---

## Sell Operation (Inverse Flow)

Selling performs the inverse:

- restores capacity to segments
- moves backward through segments
- reduces momentum

---

## Momentum Updates

### Buy

- increases momentum
- pushes system into higher price segments

### Sell

- decreases momentum
- moves system into lower price segments

---

## Regeneration System

### Purpose

- stabilize the market over time
- prevent permanent depletion or inflation

Game economies rely on balancing resource flow and preventing accumulation to maintain stability :contentReference[oaicite:1]{index=1}.

---

### Regeneration Variables

- `stock_regen_quantity`
- `stock_regen_speed`

Both are influenced by momentum.

---

### Regeneration Loop

At each tick:

1. Add regeneration quantity to current segment

2. If the segment becomes full:
   - mark segment as restored
   - move to previous (lower-priced) segment
   - decrease momentum by 1
   - update active price context

3. Continue until:
   - regen is consumed
   - or all segments are restored

---

### Key Behavior

- regeneration is segment-aware
- regeneration moves the system backward
- regeneration changes price state

---

## Invalid Behavior (Must Not Exist)

The system must NOT:

- reject operations only because quantity > stock
- use flat pricing per transaction
- ignore segment traversal
- ignore momentum transitions
- return generic errors

---

## Valid Outcomes

Operations must return domain-aware results:

- full execution
- partial execution (if allowed)
- meaningful rejection (e.g. extreme scarcity)

---

## Design Principles

- deterministic behavior
- progressive pricing
- state-driven economy
- no hidden randomness
- predictable but reactive

---

## Summary

The system is defined by:

- segmented pricing
- momentum-driven state
- progressive quote computation
- segment-aware regeneration

This creates a dynamic and stable market system driven by player actions.
