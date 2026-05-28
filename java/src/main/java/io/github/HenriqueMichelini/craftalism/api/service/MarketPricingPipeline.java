package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class MarketPricingPipeline {

    private static final long NEUTRAL_MULTIPLIER_BASIS_POINTS = 10_000L;

    private final MarketPressurePricing pressurePricing;

    MarketPricingPipeline(MarketPressurePricing pressurePricing) {
        this.pressurePricing = pressurePricing;
    }

    long buyUnitPrice(
        MarketItem item,
        long pressurePosition,
        PricingContext context
    ) {
        long pressurePrice = pressurePricing.unitPrice(item, pressurePosition);
        long driftAdjustedPrice = applyBasisPointMultiplier(
            pressurePrice,
            context.driftMultiplierBasisPoints()
        );
        long eventAdjustedPrice = applyBasisPointMultiplier(
            driftAdjustedPrice,
            context.namedEventMultiplierBasisPoints()
        );
        return clamp(
            eventAdjustedPrice,
            item.getMinUnitPrice(),
            item.getMaxUnitPrice()
        );
    }

    long sellUnitPrice(MarketItem item, long adjustedBuyUnitPrice) {
        long sellUnitPrice = BigDecimal
            .valueOf(adjustedBuyUnitPrice)
            .multiply(item.getSellPricePercentage())
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
        if (sellUnitPrice <= 0L || sellUnitPrice >= adjustedBuyUnitPrice) {
            throw new IllegalStateException(
                "Sell price percentage must produce a positive sell price below the buy price."
            );
        }
        return sellUnitPrice;
    }

    private long applyBasisPointMultiplier(
        long unitPrice,
        long multiplierBasisPoints
    ) {
        if (multiplierBasisPoints <= 0L) {
            throw new IllegalStateException(
                "Pricing multiplier must be positive."
            );
        }
        return BigDecimal
            .valueOf(unitPrice)
            .multiply(BigDecimal.valueOf(multiplierBasisPoints))
            .divide(
                BigDecimal.valueOf(NEUTRAL_MULTIPLIER_BASIS_POINTS),
                0,
                RoundingMode.HALF_UP
            )
            .longValueExact();
    }

    private long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    record PricingContext(
        long driftMultiplierBasisPoints,
        long namedEventMultiplierBasisPoints
    ) {
        static PricingContext neutral() {
            return new PricingContext(
                NEUTRAL_MULTIPLIER_BASIS_POINTS,
                NEUTRAL_MULTIPLIER_BASIS_POINTS
            );
        }
    }
}
