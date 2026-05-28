package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketActiveEventContextDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotItemDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
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
            snapshotVersion(item ->
                item.setSellPricePercentage(new BigDecimal("0.6500"))
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
    void snapshotVersion_changesWhenPublicActiveEventContextChanges() {
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections =
            projector.projections(List.of(pressureItem()));
        String baseline = projector.snapshotVersion(projections);

        String withEvent = projector.snapshotVersion(
            projections,
            new MarketActiveEventContextDTO(
                "Bumper Crop",
                "Farms are overflowing.",
                "Farming goods",
                "active now"
            )
        );

        assertNotEquals(baseline, withEvent);
    }

    @Test
    void response_ordersCategoriesByMarketCatalogOrder() {
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections =
            projector.projections(
                List.of(
                    item("diamond", "minerals", "Minerals", "Diamond"),
                    item("oak_log", "forestry", "Forestry", "Oak Log"),
                    item("wheat", "farming", "Farming", "Wheat"),
                    item(
                        "cobblestone",
                        "natural_blocks",
                        "Natural Blocks",
                        "Cobblestone"
                    ),
                    item(
                        "glowstone",
                        "decorative_blocks",
                        "Decorative Blocks",
                        "Glowstone"
                    ),
                    item("beef", "animal_products", "Animal Products", "Beef"),
                    item("bone", "mob_drops", "Mob Drops", "Bone")
                )
            );

        List<String> categoryIds = projector
            .response(projections, "snapshot")
            .categories()
            .stream()
            .map(category -> category.categoryId())
            .toList();

        assertEquals(
            List.of(
                "farming",
                "animal_products",
                "mob_drops",
                "natural_blocks",
                "decorative_blocks",
                "forestry",
                "minerals"
            ),
            categoryIds
        );
    }

    @Test
    void toSnapshotItem_derivesZeroVariationInsideSegmentZero() {
        MarketItem item = pressureItem();
        item.setNetPosition(49L);
        item.setVariationPercent(new BigDecimal("99.99"));

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals("100", snapshotItem.buyUnitEstimate());
        assertEquals("70", snapshotItem.sellUnitEstimate());
        assertEquals("0", snapshotItem.variationPercent());
    }

    @Test
    void toSnapshotItem_derivesPositiveVariationFromBuyEstimate() {
        MarketItem item = pressureItem();
        item.setNetPosition(50L);

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals("115", snapshotItem.buyUnitEstimate());
        assertEquals("81", snapshotItem.sellUnitEstimate());
        assertEquals("15", snapshotItem.variationPercent());
    }

    @Test
    void toSnapshotItem_derivesNegativeVariationFromBuyEstimate() {
        MarketItem item = pressureItem();
        item.setNetPosition(-1L);

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals("96", snapshotItem.buyUnitEstimate());
        assertEquals("67", snapshotItem.sellUnitEstimate());
        assertEquals("-4", snapshotItem.variationPercent());
    }

    @Test
    void toSnapshotItem_preservesEstimateSpreadForSmallPositivePressure() {
        MarketItem item = pressureItem();
        item.setNetPosition(7L);

        MarketSnapshotItemDTO snapshotItem = projector.toSnapshotItem(item);

        assertEquals("100", snapshotItem.buyUnitEstimate());
        assertEquals("70", snapshotItem.sellUnitEstimate());
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

    private MarketItem item(
        String itemId,
        String categoryId,
        String categoryDisplayName,
        String displayName
    ) {
        MarketItem item = pressureItem();
        item.setItemId(itemId);
        item.setCategory(category(categoryId, categoryDisplayName));
        item.setDisplayName(displayName);
        item.setIconKey(itemId.toUpperCase());
        return item;
    }

    private MarketCategory category(String categoryId, String displayName) {
        MarketCategory category = new MarketCategory();
        category.setCategoryId(categoryId);
        category.setDisplayName(displayName);
        category.setIconKey(switch (categoryId) {
            case "farming" -> "WHEAT";
            case "animal_products" -> "BEEF";
            case "mob_drops" -> "BONE";
            case "natural_blocks" -> "DIRT";
            case "decorative_blocks" -> "WHITE_WOOL";
            case "forestry" -> "OAK_LOG";
            case "minerals" -> "COAL";
            default -> "CHEST";
        });
        category.setDisplayOrder(switch (categoryId) {
            case "farming" -> 0;
            case "animal_products" -> 1;
            case "mob_drops" -> 2;
            case "natural_blocks" -> 3;
            case "decorative_blocks" -> 4;
            case "forestry" -> 5;
            case "minerals" -> 6;
            default -> Integer.MAX_VALUE;
        });
        category.setCreatedAt(Instant.parse("2026-04-12T18:30:00Z"));
        category.setUpdatedAt(Instant.parse("2026-04-12T18:30:00Z"));
        return category;
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

}
