package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class MarketTradePlanner {

    private final MarketPressurePricing pressurePricing =
        new MarketPressurePricing();

    long pressureSegment(MarketItem item, long netPosition) {
        return pressurePricing.segment(item, netPosition);
    }

    long pressureUnitPrice(MarketItem item, long pressurePosition) {
        return pressurePricing.unitPrice(item, pressurePosition);
    }

    TradePlan buyPlan(MarketItem item, long requestedQuantity) {
        recomputeDerivedProjections(item);
        long resultingNetPosition = Math.addExact(
            item.getNetPosition(),
            requestedQuantity
        );
        long totalAvailableQuantity = buyPressureCapacity(item);
        if (
            item.getMaxNetPosition() != null &&
            resultingNetPosition > item.getMaxNetPosition()
        ) {
            return unavailablePlan(totalAvailableQuantity);
        }
        return pressurePlan(
            item,
            item.getNetPosition(),
            requestedQuantity,
            Direction.UP,
            totalAvailableQuantity,
            legacyConsumptionDeltas(item, requestedQuantity)
        );
    }

    TradePlan sellPlan(MarketItem item, long requestedQuantity) {
        recomputeDerivedProjections(item);
        long resultingNetPosition = Math.subtractExact(
            item.getNetPosition(),
            requestedQuantity
        );
        long totalAvailableQuantity = sellPressureCapacity(item);
        if (
            item.getMinNetPosition() != null &&
            resultingNetPosition < item.getMinNetPosition()
        ) {
            return unavailablePlan(totalAvailableQuantity);
        }
        return pressurePlan(
            item,
            Math.subtractExact(item.getNetPosition(), 1L),
            requestedQuantity,
            Direction.DOWN,
            totalAvailableQuantity,
            legacyRestorationDeltas(item, requestedQuantity)
        );
    }

    void applyConsumption(TradePlan plan) {
        for (SegmentDelta delta : plan.deltas()) {
            delta
                .segment()
                .setRemainingCapacity(
                    delta.segment().getRemainingCapacity() - delta.quantity()
                );
        }
    }

    void applyRestoration(TradePlan plan) {
        for (SegmentDelta delta : plan.deltas()) {
            delta
                .segment()
                .setRemainingCapacity(
                    delta.segment().getRemainingCapacity() + delta.quantity()
                );
        }
    }

    void recomputeDerivedProjections(MarketItem item) {
        List<MarketSegment> segments = sortedSegments(item);
        if (segments.isEmpty()) {
            recomputePressureProjections(item);
            return;
        }

        long expectedIndex = 0L;
        long currentStock = 0L;
        int partialSegments = 0;
        SegmentState phase = SegmentState.CONSUMED;
        long buyFrontier = -1L;
        long restoreFrontier = -1L;

        for (MarketSegment segment : segments) {
            if (segment.getSegmentIndex() != expectedIndex) {
                throw invariantViolation(
                    "Segment indexes must be contiguous and start at zero."
                );
            }
            if (segment.getMaxCapacity() <= 0L) {
                throw invariantViolation(
                    "Segment max capacity must be positive."
                );
            }
            if (segment.getUnitPrice() <= 0L) {
                throw invariantViolation(
                    "Segment unit price must be positive."
                );
            }
            if (
                segment.getRemainingCapacity() < 0L ||
                segment.getRemainingCapacity() > segment.getMaxCapacity()
            ) {
                throw invariantViolation(
                    "Segment remaining capacity must stay within bounds."
                );
            }

            currentStock = Math.addExact(
                currentStock,
                segment.getRemainingCapacity()
            );
            if (segment.getRemainingCapacity() > 0L && buyFrontier == -1L) {
                buyFrontier = segment.getSegmentIndex();
            }
            if (segment.getRemainingCapacity() < segment.getMaxCapacity()) {
                restoreFrontier = segment.getSegmentIndex();
            }

            SegmentState state = stateOf(segment);
            if (state == SegmentState.PARTIAL) {
                partialSegments++;
                if (partialSegments > 1 || phase == SegmentState.UNTOUCHED) {
                    throw invariantViolation(
                        "There must be at most one partially consumed segment and no gaps."
                    );
                }
                phase = SegmentState.PARTIAL;
            } else if (state == SegmentState.UNTOUCHED) {
                phase = SegmentState.UNTOUCHED;
            } else if (phase != SegmentState.CONSUMED) {
                throw invariantViolation(
                    "Consumed segments cannot appear after partial or untouched segments."
                );
            }

            expectedIndex++;
        }

        item.setCurrentStock(currentStock);
        item.setMarketMomentum(restoreFrontier);
        item.setBuyUnitEstimate(
            frontierUnitPrice(
                segments,
                buyFrontier,
                segments.get(segments.size() - 1).getUnitPrice()
            )
        );
        item.setSellUnitEstimate(
            frontierUnitPrice(
                segments,
                restoreFrontier,
                segments.get(0).getUnitPrice()
            )
        );
    }

    private void recomputePressureProjections(MarketItem item) {
        item.setCurrentStock(0L);
        item.setMarketMomentum(
            pressurePricing.segment(item, item.getNetPosition())
        );
        item.setBuyUnitEstimate(
            pressurePricing.unitPrice(item, item.getNetPosition())
        );
        item.setSellUnitEstimate(
            pressurePricing.unitPrice(
                item,
                Math.subtractExact(item.getNetPosition(), 1L)
            )
        );
    }

    List<MarketSegment> sortedSegments(MarketItem item) {
        return item
            .getSegments()
            .stream()
            .sorted(Comparator.comparingLong(MarketSegment::getSegmentIndex))
            .toList();
    }

    private long effectiveUnitPrice(long totalPrice, long quantity) {
        return Math.floorDiv(
            Math.addExact(totalPrice, quantity - 1L),
            quantity
        );
    }

    private TradePlan pressurePlan(
        MarketItem item,
        long startPosition,
        long requestedQuantity,
        Direction direction,
        long totalAvailableQuantity,
        List<SegmentDelta> deltas
    ) {
        long remainingRequest = requestedQuantity;
        long currentPosition = startPosition;
        long totalPrice = 0L;

        while (remainingRequest > 0L) {
            long take = Math.min(
                remainingRequest,
                positionsRemainingInSegment(item, currentPosition, direction)
            );
            totalPrice = Math.addExact(
                totalPrice,
                Math.multiplyExact(
                    take,
                    pressurePricing.unitPrice(item, currentPosition)
                )
            );
            remainingRequest -= take;
            currentPosition =
                direction == Direction.UP
                    ? Math.addExact(currentPosition, take)
                    : Math.subtractExact(currentPosition, take);
        }

        return new TradePlan(
            requestedQuantity,
            effectiveUnitPrice(totalPrice, requestedQuantity),
            totalPrice,
            totalAvailableQuantity,
            deltas
        );
    }

    private List<SegmentDelta> legacyConsumptionDeltas(
        MarketItem item,
        long requestedQuantity
    ) {
        long remainingRequest = requestedQuantity;
        List<SegmentDelta> deltas = new ArrayList<>();

        for (MarketSegment segment : sortedSegments(item)) {
            if (remainingRequest <= 0L) {
                break;
            }
            if (segment.getRemainingCapacity() <= 0L) {
                continue;
            }
            long take = Math.min(
                remainingRequest,
                segment.getRemainingCapacity()
            );
            remainingRequest -= take;
            deltas.add(new SegmentDelta(segment, take));
        }
        return List.copyOf(deltas);
    }

    private List<SegmentDelta> legacyRestorationDeltas(
        MarketItem item,
        long requestedQuantity
    ) {
        long remainingRequest = requestedQuantity;
        List<SegmentDelta> deltas = new ArrayList<>();
        List<MarketSegment> segments = sortedSegments(item);

        for (
            int index = segments.size() - 1;
            index >= 0 && remainingRequest > 0L;
            index--
        ) {
            MarketSegment segment = segments.get(index);
            long restorable =
                segment.getMaxCapacity() - segment.getRemainingCapacity();
            if (restorable <= 0L) {
                continue;
            }
            long take = Math.min(remainingRequest, restorable);
            remainingRequest -= take;
            deltas.add(new SegmentDelta(segment, take));
        }
        return List.copyOf(deltas);
    }

    private long positionsRemainingInSegment(
        MarketItem item,
        long pressurePosition,
        Direction direction
    ) {
        long offset = Math.floorMod(pressurePosition, item.getSegmentSize());
        return direction == Direction.UP
            ? item.getSegmentSize() - offset
            : offset + 1L;
    }

    private TradePlan unavailablePlan(long totalAvailableQuantity) {
        return new TradePlan(
            0L,
            0L,
            0L,
            totalAvailableQuantity,
            Collections.emptyList()
        );
    }

    private long frontierUnitPrice(
        List<MarketSegment> segments,
        long frontier,
        long fallback
    ) {
        if (frontier < 0L) {
            return fallback;
        }
        return segments.get(Math.toIntExact(frontier)).getUnitPrice();
    }

    private SegmentState stateOf(MarketSegment segment) {
        if (segment.getRemainingCapacity() == 0L) {
            return SegmentState.CONSUMED;
        }
        if (segment.getRemainingCapacity() == segment.getMaxCapacity()) {
            return SegmentState.UNTOUCHED;
        }
        return SegmentState.PARTIAL;
    }

    private long buyPressureCapacity(MarketItem item) {
        if (item.getMaxNetPosition() == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, item.getMaxNetPosition() - item.getNetPosition());
    }

    private long sellPressureCapacity(MarketItem item) {
        if (item.getMinNetPosition() == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, item.getNetPosition() - item.getMinNetPosition());
    }

    private IllegalStateException invariantViolation(String message) {
        return new IllegalStateException(message);
    }

    private enum SegmentState {
        CONSUMED,
        PARTIAL,
        UNTOUCHED,
    }

    private enum Direction {
        UP,
        DOWN,
    }

    record SegmentDelta(MarketSegment segment, long quantity) {}

    record TradePlan(
        long executedQuantity,
        long unitPrice,
        long totalPrice,
        long totalAvailableQuantity,
        List<SegmentDelta> deltas
    ) {}
}
