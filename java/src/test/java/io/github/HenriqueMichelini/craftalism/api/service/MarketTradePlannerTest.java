package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MarketTradePlannerTest {

    private final MarketTradePlanner planner = new MarketTradePlanner();

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "49, 0",
        "50, 1",
        "-1, -1",
        "-50, -1",
        "-51, -2",
    })
    void pressureSegment_usesFloorDivisionForNegativePressure(
        long netPosition,
        long expectedSegment
    ) {
        MarketItem item = pressureItem(0L);

        assertEquals(
            expectedSegment,
            planner.pressureSegment(item, netPosition)
        );
    }

    @Test
    void pressureUnitPrice_pricesSegmentZeroAtBaseUnitPrice() {
        MarketItem item = pressureItem(49L);

        assertEquals(
            100L,
            planner.pressureUnitPrice(item, item.getNetPosition())
        );
    }

    @Test
    void pressureUnitPrice_positivePressureApproachesMaxUnitPrice() {
        MarketItem item = pressureItem(50_000L);

        long unitPrice = planner.pressureUnitPrice(item, item.getNetPosition());

        assertEquals(300L, unitPrice);
    }

    @Test
    void pressureUnitPrice_negativePressureApproachesMinUnitPrice() {
        MarketItem item = pressureItem(-50_000L);

        long unitPrice = planner.pressureUnitPrice(item, item.getNetPosition());

        assertEquals(50L, unitPrice);
    }

    @Test
    void pressureUnitPrice_roundsAndClampsWithinBounds() {
        MarketItem item = pressureItem(50L);

        long unitPrice = planner.pressureUnitPrice(item, item.getNetPosition());

        assertEquals(115L, unitPrice);
    }

    @Test
    void buyPlan_consumesSegmentsFromLowestPriceFrontier() {
        MarketItem item = marketItem(segment(0L, 50L, 50L, 5L), segment(1L, 50L, 50L, 6L));

        MarketTradePlanner.TradePlan plan = planner.buyPlan(item, 60L);

        assertEquals(60L, plan.executedQuantity());
        assertEquals(6L, plan.unitPrice());
        assertEquals(310L, plan.totalPrice());
        assertEquals(100L, plan.totalAvailableQuantity());
        assertEquals(2, plan.deltas().size());

        planner.applyConsumption(plan);
        planner.recomputeDerivedProjections(item);

        assertEquals(40L, item.getCurrentStock());
        assertEquals(1L, item.getMarketMomentum());
        assertEquals(6L, item.getBuyUnitEstimate());
        assertEquals(6L, item.getSellUnitEstimate());
    }

    @Test
    void sellPlan_restoresSegmentsFromHighestConsumedFrontier() {
        MarketItem item = marketItem(segment(0L, 50L, 0L, 5L), segment(1L, 50L, 20L, 6L));

        MarketTradePlanner.TradePlan plan = planner.sellPlan(item, 40L);

        assertEquals(40L, plan.executedQuantity());
        assertEquals(6L, plan.unitPrice());
        assertEquals(230L, plan.totalPrice());
        assertEquals(80L, plan.totalAvailableQuantity());
        assertEquals(2, plan.deltas().size());

        planner.applyRestoration(plan);
        planner.recomputeDerivedProjections(item);

        assertEquals(60L, item.getCurrentStock());
        assertEquals(0L, item.getMarketMomentum());
        assertEquals(5L, item.getBuyUnitEstimate());
        assertEquals(5L, item.getSellUnitEstimate());
    }

    @Test
    void recomputeDerivedProjections_rejectsMultiplePartialSegments() {
        MarketItem item = invalidMarketItem(
            segment(0L, 50L, 10L, 5L),
            segment(1L, 50L, 20L, 6L)
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> planner.recomputeDerivedProjections(item)
        );

        assertEquals(
            "There must be at most one partially consumed segment and no gaps.",
            exception.getMessage()
        );
    }

    @Test
    void recomputeDerivedProjections_rejectsNonContiguousSegments() {
        MarketItem item = invalidMarketItem(
            segment(0L, 50L, 50L, 5L),
            segment(2L, 50L, 50L, 6L)
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> planner.recomputeDerivedProjections(item)
        );

        assertEquals(
            "Segment indexes must be contiguous and start at zero.",
            exception.getMessage()
        );
    }

    private MarketItem marketItem(MarketSegment... segments) {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setCurrency("coins");
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        for (MarketSegment segment : segments) {
            item.addSegment(segment);
        }
        planner.recomputeDerivedProjections(item);
        return item;
    }

    private MarketItem invalidMarketItem(MarketSegment... segments) {
        MarketItem item = baseMarketItem();
        for (MarketSegment segment : segments) {
            item.addSegment(segment);
        }
        return item;
    }

    private MarketItem baseMarketItem() {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setCurrency("coins");
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }

    private MarketItem pressureItem(long netPosition) {
        MarketItem item = baseMarketItem();
        item.setBaseUnitPrice(100L);
        item.setMinUnitPrice(50L);
        item.setMaxUnitPrice(300L);
        item.setSegmentSize(50L);
        item.setPriceSensitivity(new BigDecimal("0.0800"));
        item.setNetPosition(netPosition);
        return item;
    }

    private MarketSegment segment(
        long index,
        long maxCapacity,
        long remainingCapacity,
        long unitPrice
    ) {
        MarketSegment segment = new MarketSegment();
        segment.setSegmentIndex(index);
        segment.setMaxCapacity(maxCapacity);
        segment.setRemainingCapacity(remainingCapacity);
        segment.setUnitPrice(unitPrice);
        return segment;
    }
}
