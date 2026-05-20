package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketCatalogInitializerTest {

    @Mock
    private MarketItemRepository marketItemRepository;

    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();

    @Test
    void initializeCatalogIfEmpty_seedsPressureCatalogDefaults() {
        when(marketItemRepository.count()).thenReturn(0L);

        MarketCatalogInitializer initializer = new MarketCatalogInitializer(
            marketItemRepository,
            new DefaultMarketCatalog(),
            tradePlanner
        );

        initializer.initializeCatalogIfEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MarketItem>> itemCaptor =
            ArgumentCaptor.forClass(Iterable.class);
        verify(marketItemRepository).saveAll(itemCaptor.capture());

        List<MarketItem> savedItems = new ArrayList<>();
        itemCaptor.getValue().forEach(savedItems::add);
        assertEquals(
            new DefaultMarketCatalog().items().size(),
            savedItems.size()
        );
        MarketItem wheat = savedItems
            .stream()
            .filter(item -> item.getItemId().equals("wheat"))
            .findFirst()
            .orElseThrow();

        assertEquals(50000L, wheat.getBaseUnitPrice());
        assertEquals(25000L, wheat.getMinUnitPrice());
        assertEquals(150000L, wheat.getMaxUnitPrice());
        assertEquals(50L, wheat.getSegmentSize());
        assertEquals(
            0,
            new BigDecimal("0.0800").compareTo(wheat.getPriceSensitivity())
        );
        assertEquals(1L, wheat.getBaseRegenQuantity());
        assertEquals(60L, wheat.getRegenIntervalSeconds());
        assertEquals(0L, wheat.getNetPosition());
        assertNull(wheat.getMinNetPosition());
        assertNull(wheat.getMaxNetPosition());

        MarketItem diamond = savedItems
            .stream()
            .filter(item -> item.getItemId().equals("diamond"))
            .findFirst()
            .orElseThrow();
        assertEquals("mining", diamond.getCategoryId());
        assertEquals("Diamond", diamond.getDisplayName());
        assertEquals(900000L, diamond.getBaseUnitPrice());
        assertEquals(450000L, diamond.getMinUnitPrice());
        assertEquals(2700000L, diamond.getMaxUnitPrice());
    }

    @Test
    void defaultMarketCatalog_containsRequestedLogsOresAndMobDrops() {
        List<MarketSeedItem> items = new DefaultMarketCatalog().items();

        assertSeed(items, "spruce_log", "forestry", "Forestry", 25_000L);
        assertSeed(items, "oak_log", "forestry", "Forestry", 25_000L);
        assertSeed(items, "birch_log", "forestry", "Forestry", 25_000L);
        assertSeed(items, "jungle_log", "forestry", "Forestry", 25_000L);
        assertSeed(items, "acacia_log", "forestry", "Forestry", 45_000L);
        assertSeed(items, "dark_oak_log", "forestry", "Forestry", 45_000L);
        assertSeed(items, "mangrove_log", "forestry", "Forestry", 75_000L);
        assertSeed(items, "cherry_log", "forestry", "Forestry", 75_000L);
        assertSeed(items, "pale_oak_log", "forestry", "Forestry", 75_000L);

        assertSeed(items, "redstone_dust", "mining", "Mining", 35_000L);
        assertSeed(items, "lapis_lazuli", "mining", "Mining", 35_000L);
        assertSeed(items, "emerald", "mining", "Mining", 750_000L);

        assertSeed(items, "rotten_flesh", "mob_drops", "Mob Drops", 2_000L);
        assertSeed(items, "bone", "mob_drops", "Mob Drops", 8_000L);
        assertSeed(items, "string", "mob_drops", "Mob Drops", 8_000L);
        assertSeed(items, "gunpowder", "mob_drops", "Mob Drops", 18_000L);
        assertSeed(items, "ender_pearl", "mob_drops", "Mob Drops", 45_000L);
        assertSeed(items, "blaze_rod", "mob_drops", "Mob Drops", 60_000L);
        assertSeed(items, "ghast_tear", "mob_drops", "Mob Drops", 90_000L);
        assertSeed(
            items,
            "wither_skeleton_skull",
            "mob_drops",
            "Mob Drops",
            250_000L
        );
        assertSeed(
            items,
            "totem_of_undying",
            "mob_drops",
            "Mob Drops",
            300_000L
        );
        assertSeed(items, "trident", "mob_drops", "Mob Drops", 350_000L);
    }

    @ParameterizedTest
    @MethodSource("invalidPressureSeeds")
    void marketSeedItem_rejectsInvalidPressureConfiguration(
        MarketSeedItemBuilder seedBuilder
    ) {
        assertThrows(IllegalArgumentException.class, seedBuilder::build);
    }

    @Test
    void initializeCatalogIfEmpty_recomputesExistingPressureProjections() {
        MarketItem item = pressureItem();
        item.setBuyUnitEstimate(999L);
        item.setSellUnitEstimate(888L);
        item.setCurrentStock(777L);
        item.setMarketMomentum(666L);
        when(marketItemRepository.count()).thenReturn(1L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );

        MarketCatalogInitializer initializer = new MarketCatalogInitializer(
            marketItemRepository,
            new DefaultMarketCatalog(),
            tradePlanner
        );

        initializer.initializeCatalogIfEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MarketItem>> itemCaptor =
            ArgumentCaptor.forClass(Iterable.class);
        verify(marketItemRepository).saveAll(itemCaptor.capture());

        List<MarketItem> savedItems = new ArrayList<>();
        itemCaptor.getValue().forEach(savedItems::add);
        MarketItem savedItem = savedItems.get(0);

        assertEquals(0L, savedItem.getCurrentStock());
        assertEquals(1L, savedItem.getMarketMomentum());
        assertEquals(115L, savedItem.getBuyUnitEstimate());
        assertEquals(100L, savedItem.getSellUnitEstimate());
    }

    @Test
    void initializeCatalogIfEmpty_appendsMissingDefaultItems() {
        when(marketItemRepository.count()).thenReturn(1L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(pressureItem())
        );

        MarketCatalogInitializer initializer = new MarketCatalogInitializer(
            marketItemRepository,
            new DefaultMarketCatalog(),
            tradePlanner
        );

        initializer.initializeCatalogIfEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MarketItem>> itemCaptor =
            ArgumentCaptor.forClass(Iterable.class);
        verify(marketItemRepository).saveAll(itemCaptor.capture());

        List<MarketItem> savedItems = new ArrayList<>();
        itemCaptor.getValue().forEach(savedItems::add);
        MarketItem diamond = savedItems
            .stream()
            .filter(item -> item.getItemId().equals("diamond"))
            .findFirst()
            .orElseThrow();

        assertEquals(0L, diamond.getNetPosition());
        assertEquals(900000L, diamond.getBuyUnitEstimate());
        assertEquals("coins", diamond.getCurrency());
    }

    @Test
    void initializeCatalogIfEmpty_doesNotReadSegmentsForEmptyDefaultCatalog() {
        when(marketItemRepository.count()).thenReturn(0L);

        MarketCatalogInitializer initializer = new MarketCatalogInitializer(
            marketItemRepository,
            new DefaultMarketCatalog(),
            tradePlanner
        );

        initializer.initializeCatalogIfEmpty();

        verify(marketItemRepository, never()).findAllForMarketRead();
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
        item.setNetPosition(50L);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }

    private static List<MarketSeedItemBuilder> invalidPressureSeeds() {
        return List.of(
            validSeedBuilder().baseUnitPrice(0L),
            validSeedBuilder().minUnitPrice(0L),
            validSeedBuilder().minUnitPrice(101L),
            validSeedBuilder().maxUnitPrice(99L),
            validSeedBuilder().segmentSize(0L),
            seedBuilderWithoutPriceSensitivity(),
            validSeedBuilder().priceSensitivity("0.0000"),
            validSeedBuilder().baseRegenQuantity(-1L),
            validSeedBuilder().regenIntervalSeconds(0L),
            validSeedBuilder().minNetPosition(1L),
            validSeedBuilder().maxNetPosition(-1L),
            validSeedBuilder().minNetPosition(-10L).maxNetPosition(-20L)
        );
    }

    private static MarketSeedItemBuilder seedBuilderWithoutPriceSensitivity() {
        return new MarketSeedItemBuilder()
            .itemId("test_item")
            .categoryId("test")
            .categoryDisplayName("Test")
            .displayName("Test Item")
            .iconKey("TEST_ITEM")
            .variationPercent("0.0")
            .baseUnitPrice(100L)
            .minUnitPrice(50L)
            .maxUnitPrice(300L)
            .segmentSize(50L)
            .baseRegenQuantity(1L)
            .regenIntervalSeconds(60L);
    }

    private static MarketSeedItemBuilder validSeedBuilder() {
        return new MarketSeedItemBuilder()
            .itemId("test_item")
            .categoryId("test")
            .categoryDisplayName("Test")
            .displayName("Test Item")
            .iconKey("TEST_ITEM")
            .variationPercent("0.0")
            .baseUnitPrice(100L)
            .minUnitPrice(50L)
            .maxUnitPrice(300L)
            .segmentSize(50L)
            .priceSensitivity("0.0800")
            .baseRegenQuantity(1L)
            .regenIntervalSeconds(60L);
    }

    private static void assertSeed(
        List<MarketSeedItem> items,
        String itemId,
        String categoryId,
        String categoryDisplayName,
        long baseUnitPrice
    ) {
        MarketSeedItem item = items
            .stream()
            .filter(seed -> seed.itemId().equals(itemId))
            .findFirst()
            .orElseThrow();

        assertEquals(categoryId, item.categoryId());
        assertEquals(categoryDisplayName, item.categoryDisplayName());
        assertEquals(baseUnitPrice, item.baseUnitPrice());
        assertEquals(baseUnitPrice / 2L, item.minUnitPrice());
        assertEquals(baseUnitPrice * 3L, item.maxUnitPrice());
        assertTrue(item.baseRegenQuantity() > 0L);
    }
}
