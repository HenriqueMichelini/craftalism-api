# Market Aggregate — Superseded

This document has been superseded by `docs/market-pressure-ladder-sigmoid-pricing.md`.

The previous finite segment-capacity aggregate is no longer the target market design. New implementation work must follow the pressure-ladder plan, where `MarketItem.netPosition` is the authoritative mutable pricing state and virtual segments are derived deterministically from pressure state.

Historical note:

- the old model used persisted `market_segments`
- `currentStock` meant `sum(remainingCapacity)`
- buys consumed finite segment capacity
- sells restored finite segment capacity

Do not use those old rules for new implementation work except when writing migration or audit code for legacy state.
