package io.github.HenriqueMichelini.craftalism.api.market.domain.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventPricingService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.pricing.MarketPressurePricing;
import io.github.HenriqueMichelini.craftalism.api.market.domain.pricing.MarketPricingPipeline;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
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
    void pricingPipeline_neutralContextMatchesPressureOnlyBuyAndSellPrices() {
        MarketItem item = pressureItem(50L);
        MarketPricingPipeline pipeline = new MarketPricingPipeline(
            new MarketPressurePricing()
        );
        MarketPricingPipeline.PricingContext context =
            MarketPricingPipeline.PricingContext.neutral();

        long buyUnitPrice = pipeline.buyUnitPrice(
            item,
            item.getNetPosition(),
            context
        );

        assertEquals(115L, buyUnitPrice);
        assertEquals(81L, pipeline.sellUnitPrice(item, buyUnitPrice));
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
        assertEquals(70L, plan.unitPrice());
        assertEquals(140L, plan.totalPrice());
        assertEquals(Long.MAX_VALUE, plan.totalAvailableQuantity());
    }

    @Test
    void sellPlan_pricesNextPositionBelowNegativeSegmentBoundary() {
        MarketItem item = pressureItem(-50L);

        MarketTradePlanner.TradePlan plan = planner.sellPlan(item, 1L);

        assertEquals(1L, plan.executedQuantity());
        assertEquals(67L, plan.unitPrice());
        assertEquals(67L, plan.totalPrice());
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
        item.setVariationPercent(new BigDecimal("99.99"));

        planner.recomputeDerivedProjections(item);

        assertEquals(0L, item.getCurrentStock());
        assertEquals(1L, item.getMarketMomentum());
        assertEquals(115L, item.getBuyUnitEstimate());
        assertEquals(81L, item.getSellUnitEstimate());
        assertEquals(
            0,
            new BigDecimal("15.00").compareTo(item.getVariationPercent())
        );
    }

    @Test
    void recomputeDerivedProjections_reportsZeroVariationInsideSegmentZero() {
        MarketItem item = pressureItem(49L);
        item.setVariationPercent(new BigDecimal("2.3"));

        planner.recomputeDerivedProjections(item);

        assertEquals(100L, item.getBuyUnitEstimate());
        assertEquals(70L, item.getSellUnitEstimate());
        assertEquals(
            0,
            BigDecimal.ZERO.compareTo(item.getVariationPercent())
        );
    }

    @Test
    void recomputeDerivedProjections_preservesSpreadForSmallPositivePressure() {
        MarketItem item = pressureItem(7L);

        planner.recomputeDerivedProjections(item);

        assertEquals(100L, item.getBuyUnitEstimate());
        assertEquals(70L, item.getSellUnitEstimate());
    }

    @Test
    void recomputeDerivedProjections_preservesSpreadForNegativePressure() {
        MarketItem item = pressureItem(-1L);

        planner.recomputeDerivedProjections(item);

        assertEquals(96L, item.getBuyUnitEstimate());
        assertEquals(67L, item.getSellUnitEstimate());
    }

    @Test
    void recomputeDerivedProjections_appliesSellPercentageAtMinimumBuyEstimate() {
        MarketItem item = pressureItem(-50_000L);

        planner.recomputeDerivedProjections(item);

        assertEquals(50L, item.getBuyUnitEstimate());
        assertEquals(35L, item.getSellUnitEstimate());
    }

    @Test
    void recomputeDerivedProjections_keepsStablePercentageAfterLargeBuyPressure() {
        MarketItem item = pressureItem(1_500L);

        planner.recomputeDerivedProjections(item);

        assertEquals(
            BigDecimal
                .valueOf(item.getBuyUnitEstimate())
                .multiply(new BigDecimal("0.7000"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact(),
            item.getSellUnitEstimate()
        );
    }

    @Test
    void recomputeDerivedProjections_reportsNegativeVariationFromBuyEstimate() {
        MarketItem item = pressureItem(-1L);
        item.setVariationPercent(new BigDecimal("2.3"));

        planner.recomputeDerivedProjections(item);

        assertEquals(96L, item.getBuyUnitEstimate());
        assertEquals(67L, item.getSellUnitEstimate());
        assertEquals(
            0,
            new BigDecimal("-4.00").compareTo(item.getVariationPercent())
        );
    }

    @Test
    void recomputeDerivedProjections_appliesDriftBeforeSellPercentageAndVariation() {
        MarketItem item = pressureItem(0L);
        item.setDriftMultiplierBasisPoints(10_600L);

        planner.recomputeDerivedProjections(item);

        assertEquals(106L, item.getBuyUnitEstimate());
        assertEquals(74L, item.getSellUnitEstimate());
        assertEquals(
            0,
            new BigDecimal("6.00").compareTo(item.getVariationPercent())
        );
    }

    @Test
    void recomputeDerivedProjections_appliesActiveNamedEventAfterDrift() {
        MarketTradePlanner eventPlanner = new MarketTradePlanner(
            eventPricingService(categoryEvent(12_000))
        );
        MarketItem item = pressureItem(0L);
        item.setDriftMultiplierBasisPoints(10_500L);

        eventPlanner.recomputeDerivedProjections(item);

        assertEquals(126L, item.getBuyUnitEstimate());
        assertEquals(88L, item.getSellUnitEstimate());
        assertEquals(
            0,
            new BigDecimal("26.00").compareTo(item.getVariationPercent())
        );
    }

    @Test
    void buyPlan_recordsActiveEventPricingMetadata() {
        MarketEventInstance event = categoryEvent(12_000);
        MarketTradePlanner eventPlanner = new MarketTradePlanner(
            eventPricingService(event)
        );
        MarketItem item = pressureItem(0L);

        MarketTradePlanner.TradePlan plan = eventPlanner.buyPlan(item, 1L);

        assertEquals(120L, plan.unitPrice());
        assertEquals(120L, plan.totalPrice());
        assertEquals(0L, plan.driftRevision());
        assertEquals(event.getId(), plan.namedEventInstanceId());
        assertEquals(event.getEffectVersion(), plan.eventEffectVersion());
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
        item.setDriftMultiplierBasisPoints(10_000L);
        item.setDriftRevision(0L);
        item.setDriftEvaluatedAt(Instant.parse("2026-04-12T18:29:42Z"));
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

    private MarketEventPricingService eventPricingService(
        MarketEventInstance event
    ) {
        MarketEventInstanceRepository repository = mock(
            MarketEventInstanceRepository.class
        );
        when(repository.findEffectiveActive(any())).thenReturn(
            Optional.of(event)
        );
        return new MarketEventPricingService(
            new MarketEventLifecycleService(repository)
        );
    }

    private MarketEventInstance categoryEvent(int effectBasisPoints) {
        MarketEventInstance event = new MarketEventInstance();
        event.setId(42L);
        event.setTemplateId("farming_bumper_crop");
        event.setSource(MarketEventSource.SCHEDULER);
        event.setScope(MarketEventScope.CATEGORY);
        event.setSelectedCategoryId("farming");
        event.setEffectBasisPoints(effectBasisPoints);
        event.setEffectVersion(3);
        event.setBlocking(false);
        event.setStartedAt(Instant.parse("2026-04-12T18:00:00Z"));
        event.setEndsAt(Instant.parse("2026-04-12T19:00:00Z"));
        event.setStatus(MarketEventStatus.ACTIVE);
        event.setCreatedAt(Instant.parse("2026-04-12T18:00:00Z"));
        event.setUpdatedAt(Instant.parse("2026-04-12T18:00:00Z"));
        return event;
    }
}
