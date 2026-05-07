package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotItemDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class MarketSnapshotProjectorTest {

    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();
    private final MarketSnapshotProjector projector =
        new MarketSnapshotProjector(tradePlanner);

    @Test
    void snapshotVersion_changesWhenAuthoritativePressureStateChanges() {
        String baseline = snapshotVersion(item -> {});

        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setNetPosition(1L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setMinNetPosition(-100L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setMaxNetPosition(100L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setBlocked(true))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setOperating(false))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item ->
                item.setLastUpdatedAt(Instant.parse("2026-04-12T18:31:00Z"))
            )
        );
    }

    @Test
    void snapshotVersion_changesWhenTradeAffectingPressureConfigChanges() {
        String baseline = snapshotVersion(item -> {});

        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setBaseUnitPrice(101L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setMinUnitPrice(51L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setMaxUnitPrice(301L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setSegmentSize(25L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item ->
                item.setPriceSensitivity(new BigDecimal("0.1200"))
            )
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setBaseRegenQuantity(2L))
        );
        assertNotEquals(
            baseline,
            snapshotVersion(item -> item.setRegenIntervalSeconds(30L))
        );
    }

    @Test
    void snapshotVersion_staysStableForDerivedOnlyRecalculation() {
        MarketItem item = pressureItem();
        String baseline = snapshotVersion(item);

        item.setBuyUnitEstimate(999L);
        item.setSellUnitEstimate(888L);
        item.setCurrentStock(777L);
        item.setMarketMomentum(666L);
        item.setVariationPercent(new BigDecimal("99.99"));

        assertEquals(baseline, snapshotVersion(item));
    }

    @Test
    void toSnapshotItem_derivesZeroVariationInsideSegmentZero() {
        MarketItem item = pressureItem();
        item.setNetPosition(49L);
        item.setVariationPercent(new BigDecimal("99.99"));

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals("100", snapshotItem.buyUnitEstimate());
        assertEquals("0", snapshotItem.variationPercent());
    }

    @Test
    void toSnapshotItem_derivesPositiveVariationFromBuyEstimate() {
        MarketItem item = pressureItem();
        item.setNetPosition(50L);

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals("115", snapshotItem.buyUnitEstimate());
        assertEquals("15", snapshotItem.variationPercent());
    }

    @Test
    void toSnapshotItem_derivesNegativeVariationFromBuyEstimate() {
        MarketItem item = pressureItem();
        item.setNetPosition(-1L);

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals("96", snapshotItem.buyUnitEstimate());
        assertEquals("-4", snapshotItem.variationPercent());
    }

    @Test
    void snapshotVersion_staysStableWhenLegacySegmentRowsChange() {
        MarketItem item = pressureItem();
        MarketSegment segment = segment(0L, 50L, 50L, 100L);
        item.setSegments(List.of(segment));
        String baseline = snapshotVersion(item);

        segment.setRemainingCapacity(10L);
        segment.setUnitPrice(125L);

        assertEquals(baseline, snapshotVersion(item));
    }

    @Test
    void toSnapshotItem_projectsPositivePressureFields() {
        MarketItem item = pressureItem();
        item.setNetPosition(50L);

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals(50L, snapshotItem.marketPressure());
        assertEquals(1L, snapshotItem.marketSegment());
        assertEquals(50L, snapshotItem.pressureMagnitude());
    }

    @Test
    void toSnapshotItem_projectsZeroPressureFields() {
        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(
            pressureItem()
        );

        assertEquals(0L, snapshotItem.marketPressure());
        assertEquals(0L, snapshotItem.marketSegment());
        assertEquals(0L, snapshotItem.pressureMagnitude());
    }

    @Test
    void toSnapshotItem_projectsNegativePressureFields() {
        MarketItem item = pressureItem();
        item.setNetPosition(-51L);

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals(-51L, snapshotItem.marketPressure());
        assertEquals(-2L, snapshotItem.marketSegment());
        assertEquals(51L, snapshotItem.pressureMagnitude());
    }

    private String snapshotVersion(Consumer<MarketItem> customizer) {
        MarketItem item = pressureItem();
        customizer.accept(item);
        return snapshotVersion(item);
    }

    private String snapshotVersion(MarketItem item) {
        return projector.snapshotVersion(
            projector.projections(List.of(item))
        );
    }

    private MarketItem pressureItem() {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setCurrency("coins");
        item.setBaseUnitPrice(100L);
        item.setMinUnitPrice(50L);
        item.setMaxUnitPrice(300L);
        item.setSegmentSize(50L);
        item.setPriceSensitivity(new BigDecimal("0.0800"));
        item.setBaseRegenQuantity(1L);
        item.setRegenIntervalSeconds(60L);
        item.setNetPosition(0L);
        item.setVariationPercent(new BigDecimal("2.30"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:30:00Z"));
        tradePlanner.recomputeDerivedProjections(item);
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
