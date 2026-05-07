package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
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
    void buyPlan_pricesVirtualPositionsAcrossPositiveBoundary() {
        MarketItem item = pressureItem(49L);

        MarketTradePlanner.TradePlan plan = planner.buyPlan(item, 2L);

        assertEquals(2L, plan.executedQuantity());
        assertEquals(108L, plan.unitPrice());
        assertEquals(215L, plan.totalPrice());
        assertEquals(Long.MAX_VALUE, plan.totalAvailableQuantity());
    }

    @Test
    void buyPlan_pricesVirtualPositionsFromNegativeThroughPositiveBoundary() {
        MarketItem item = pressureItem(-1L);

        MarketTradePlanner.TradePlan plan = planner.buyPlan(item, 52L);

        assertEquals(52L, plan.executedQuantity());
        assertEquals(101L, plan.unitPrice());
        assertEquals(5_211L, plan.totalPrice());
        assertEquals(Long.MAX_VALUE, plan.totalAvailableQuantity());
    }

    @Test
    void buyPlan_pricesVirtualPositionsAcrossNegativeBoundary() {
        MarketItem item = pressureItem(-51L);

        MarketTradePlanner.TradePlan plan = planner.buyPlan(item, 2L);

        assertEquals(2L, plan.executedQuantity());
        assertEquals(95L, plan.unitPrice());
        assertEquals(189L, plan.totalPrice());
        assertEquals(Long.MAX_VALUE, plan.totalAvailableQuantity());
    }

    @Test
    void sellPlan_pricesVirtualPositionsAcrossZeroIntoNegativeBoundary() {
        MarketItem item = pressureItem(1L);

        MarketTradePlanner.TradePlan plan = planner.sellPlan(item, 2L);

        assertEquals(2L, plan.executedQuantity());
        assertEquals(98L, plan.unitPrice());
        assertEquals(196L, plan.totalPrice());
        assertEquals(Long.MAX_VALUE, plan.totalAvailableQuantity());
    }

    @Test
    void sellPlan_pricesNextPositionBelowNegativeSegmentBoundary() {
        MarketItem item = pressureItem(-50L);

        MarketTradePlanner.TradePlan plan = planner.sellPlan(item, 1L);

        assertEquals(1L, plan.executedQuantity());
        assertEquals(93L, plan.unitPrice());
        assertEquals(93L, plan.totalPrice());
        assertEquals(Long.MAX_VALUE, plan.totalAvailableQuantity());
    }

    @Test
    void buyPlan_rejectsOnlyConfiguredMaximumPressureBound() {
        MarketItem item = pressureItem(49L);
        item.setMaxNetPosition(50L);

        MarketTradePlanner.TradePlan plan = planner.buyPlan(item, 2L);

        assertEquals(0L, plan.executedQuantity());
        assertEquals(0L, plan.unitPrice());
        assertEquals(0L, plan.totalPrice());
        assertEquals(1L, plan.totalAvailableQuantity());
    }

    @Test
    void sellPlan_rejectsOnlyConfiguredMinimumPressureBound() {
        MarketItem item = pressureItem(-49L);
        item.setMinNetPosition(-50L);

        MarketTradePlanner.TradePlan plan = planner.sellPlan(item, 2L);

        assertEquals(0L, plan.executedQuantity());
        assertEquals(0L, plan.unitPrice());
        assertEquals(0L, plan.totalPrice());
        assertEquals(1L, plan.totalAvailableQuantity());
    }

    @Test
    void buyPlan_rejectsOverflow() {
        MarketItem item = pressureItem(Long.MAX_VALUE);

        assertThrows(ArithmeticException.class, () -> planner.buyPlan(item, 1L));
    }

    @Test
    void sellPlan_rejectsOverflow() {
        MarketItem item = pressureItem(Long.MIN_VALUE);

        assertThrows(
            ArithmeticException.class,
            () -> planner.sellPlan(item, 1L)
        );
    }

    @Test
    void recomputeDerivedProjections_usesPressureState() {
        MarketItem item = pressureItem(50L);
        item.setBuyUnitEstimate(999L);
        item.setSellUnitEstimate(888L);
        item.setCurrentStock(777L);
        item.setMarketMomentum(666L);

        planner.recomputeDerivedProjections(item);

        assertEquals(0L, item.getCurrentStock());
        assertEquals(1L, item.getMarketMomentum());
        assertEquals(115L, item.getBuyUnitEstimate());
        assertEquals(100L, item.getSellUnitEstimate());
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
}
