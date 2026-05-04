package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MarketTradePlanner {

    TradePlan buyPlan(MarketItem item, long requestedQuantity) {
        recomputeDerivedProjections(item);
        long remainingRequest = requestedQuantity;
        long totalPrice = 0L;
        long executedQuantity = 0L;
        long totalAvailableQuantity = item.getCurrentStock();
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
            totalPrice = Math.addExact(
                totalPrice,
                Math.multiplyExact(take, segment.getUnitPrice())
            );
            executedQuantity = Math.addExact(executedQuantity, take);
            remainingRequest -= take;
            deltas.add(new SegmentDelta(segment, take));
        }

        long unitPrice =
            executedQuantity == 0L
                ? 0L
                : effectiveUnitPrice(totalPrice, executedQuantity);
        return new TradePlan(
            executedQuantity,
            unitPrice,
            totalPrice,
            totalAvailableQuantity,
            deltas
        );
    }

    TradePlan sellPlan(MarketItem item, long requestedQuantity) {
        recomputeDerivedProjections(item);
        long remainingRequest = requestedQuantity;
        long totalPrice = 0L;
        long executedQuantity = 0L;
        long totalAvailableQuantity = totalRestorableCapacity(item);
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
            totalPrice = Math.addExact(
                totalPrice,
                Math.multiplyExact(take, segment.getUnitPrice())
            );
            executedQuantity = Math.addExact(executedQuantity, take);
            remainingRequest -= take;
            deltas.add(new SegmentDelta(segment, take));
        }

        long unitPrice =
            executedQuantity == 0L
                ? 0L
                : effectiveUnitPrice(totalPrice, executedQuantity);
        return new TradePlan(
            executedQuantity,
            unitPrice,
            totalPrice,
            totalAvailableQuantity,
            deltas
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
            throw invariantViolation(
                "Market item must have at least one segment."
            );
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

    private long totalRestorableCapacity(MarketItem item) {
        long total = 0L;
        for (MarketSegment segment : item.getSegments()) {
            total = Math.addExact(
                total,
                segment.getMaxCapacity() - segment.getRemainingCapacity()
            );
        }
        return total;
    }

    private IllegalStateException invariantViolation(String message) {
        return new IllegalStateException(message);
    }

    private enum SegmentState {
        CONSUMED,
        PARTIAL,
        UNTOUCHED,
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
