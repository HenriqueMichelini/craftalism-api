package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.math.BigDecimal;
import java.math.RoundingMode;

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
            totalAvailableQuantity
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
            totalAvailableQuantity
        );
    }

    void recomputeDerivedProjections(MarketItem item) {
        long buyUnitEstimate = pressurePricing.unitPrice(
            item,
            item.getNetPosition()
        );
        item.setCurrentStock(0L);
        item.setMarketMomentum(
            pressurePricing.segment(item, item.getNetPosition())
        );
        item.setBuyUnitEstimate(buyUnitEstimate);
        item.setSellUnitEstimate(
            pressurePricing.unitPrice(
                item,
                Math.subtractExact(item.getNetPosition(), 1L)
            )
        );
        item.setVariationPercent(variationPercent(item, buyUnitEstimate));
    }

    private BigDecimal variationPercent(
        MarketItem item,
        long buyUnitEstimate
    ) {
        return BigDecimal
            .valueOf(
                Math.subtractExact(buyUnitEstimate, item.getBaseUnitPrice())
            )
            .multiply(BigDecimal.valueOf(100L))
            .divide(
                BigDecimal.valueOf(item.getBaseUnitPrice()),
                2,
                RoundingMode.HALF_UP
            );
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
        long totalAvailableQuantity
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
            totalAvailableQuantity
        );
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
            totalAvailableQuantity
        );
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

    private enum Direction {
        UP,
        DOWN,
    }

    record TradePlan(
        long executedQuantity,
        long unitPrice,
        long totalPrice,
        long totalAvailableQuantity
    ) {}
}
