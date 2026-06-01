package io.github.HenriqueMichelini.craftalism.api.market.domain.pricing;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;

public final class MarketPressurePricing {

    public long segment(MarketItem item, long netPosition) {
        return Math.floorDiv(netPosition, segmentSize(item));
    }

    public long unitPrice(MarketItem item, long pressurePosition) {
        long segment = segment(item, pressurePosition);
        if (segment == 0L) {
            return item.getBaseUnitPrice();
        }

        double sensitivity = item.getPriceSensitivity().doubleValue();
        double segmentMagnitude = segment == Long.MIN_VALUE
            ? (double) Long.MAX_VALUE
            : Math.abs(segment);
        double pressure = 1.0D - Math.exp(-sensitivity * segmentMagnitude);
        double rawPrice =
            segment > 0L
                ? item.getBaseUnitPrice() +
                (item.getMaxUnitPrice() - item.getBaseUnitPrice()) * pressure
                : item.getBaseUnitPrice() -
                (item.getBaseUnitPrice() - item.getMinUnitPrice()) * pressure;

        return clamp(
            Math.round(rawPrice),
            item.getMinUnitPrice(),
            item.getMaxUnitPrice()
        );
    }

    private long segmentSize(MarketItem item) {
        long segmentSize = item.getSegmentSize();
        if (segmentSize <= 0L) {
            throw new IllegalStateException("Segment size must be positive.");
        }
        return segmentSize;
    }

    private long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
