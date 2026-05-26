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
        assertEquals(
            0,
            new BigDecimal("0.7000").compareTo(wheat.getSellPricePercentage())
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
        assertEquals("minerals", diamond.getCategoryId());
        assertEquals("Diamond", diamond.getDisplayName());
        assertEquals(900000L, diamond.getBaseUnitPrice());
        assertEquals(450000L, diamond.getMinUnitPrice());
        assertEquals(2700000L, diamond.getMaxUnitPrice());
    }

    @Test
    void defaultMarketCatalog_containsRequestedLogsCategoriesAndMobDrops() {
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

        assertCategoryOrder(
            items,
            "minerals",
            List.of(
                "coal",
                "iron_ingot",
                "gold_ingot",
                "redstone_dust",
                "lapis_lazuli",
                "emerald",
                "diamond"
            )
        );
        assertSeed(items, "coal", "minerals", "Minerals", 35_000L);
        assertSeed(items, "iron_ingot", "minerals", "Minerals", 140_000L);
        assertSeed(items, "gold_ingot", "minerals", "Minerals", 220_000L);
        assertSeed(items, "redstone_dust", "minerals", "Minerals", 35_000L);
        assertSeed(items, "lapis_lazuli", "minerals", "Minerals", 35_000L);
        assertSeed(items, "emerald", "minerals", "Minerals", 750_000L);
        assertSeed(items, "diamond", "minerals", "Minerals", 900_000L);

        assertCategoryOrder(
            items,
            "natural_blocks",
            List.of(
                "dirt",
                "cobblestone",
                "sand",
                "gravel",
                "andesite",
                "diorite",
                "granite",
                "deepslate"
            )
        );
        assertSeed(items, "dirt", "natural_blocks", "Natural Blocks", 4_000L);
        assertSeed(
            items,
            "cobblestone",
            "natural_blocks",
            "Natural Blocks",
            4_000L
        );
        assertSeed(items, "sand", "natural_blocks", "Natural Blocks", 4_000L);
        assertSeed(items, "gravel", "natural_blocks", "Natural Blocks", 4_000L);
        assertSeed(
            items,
            "andesite",
            "natural_blocks",
            "Natural Blocks",
            4_000L
        );
        assertSeed(
            items,
            "diorite",
            "natural_blocks",
            "Natural Blocks",
            4_000L
        );
        assertSeed(
            items,
            "granite",
            "natural_blocks",
            "Natural Blocks",
            4_000L
        );
        assertSeed(
            items,
            "deepslate",
            "natural_blocks",
            "Natural Blocks",
            6_000L
        );

        assertCategoryOrder(
            items,
            "decorative_blocks",
            List.of("wool", "terracotta", "glowstone", "sea_lantern")
        );
        assertSeed(
            items,
            "wool",
            "decorative_blocks",
            "Decorative Blocks",
            20_000L
        );
        assertSeed(
            items,
            "terracotta",
            "decorative_blocks",
            "Decorative Blocks",
            25_000L
        );
        assertSeed(
            items,
            "glowstone",
            "decorative_blocks",
            "Decorative Blocks",
            35_000L
        );
        assertSeed(
            items,
            "sea_lantern",
            "decorative_blocks",
            "Decorative Blocks",
            45_000L
        );

        assertSeed(
            items,
            "beef",
            "animal_products",
            "Animal Products",
            12_000L
        );
        assertSeed(
            items,
            "porkchop",
            "animal_products",
            "Animal Products",
            12_000L
        );
        assertSeed(
            items,
            "chicken",
            "animal_products",
            "Animal Products",
            8_000L
        );
        assertSeed(
            items,
            "rabbit",
            "animal_products",
            "Animal Products",
            14_000L
        );
        assertSeed(items, "cod", "animal_products", "Animal Products", 6_000L);
        assertSeed(
            items,
            "salmon",
            "animal_products",
            "Animal Products",
            8_000L
        );
        assertSeed(
            items,
            "mutton",
            "animal_products",
            "Animal Products",
            10_000L
        );
        assertSeed(
            items,
            "leather",
            "animal_products",
            "Animal Products",
            12_000L
        );
        assertSeed(items, "feather", "animal_products", "Animal Products", 6_000L);
        assertSeed(items, "egg", "animal_products", "Animal Products", 6_000L);
        assertSeed(
            items,
            "rabbit_hide",
            "animal_products",
            "Animal Products",
            8_000L
        );

        assertSeed(items, "rotten_flesh", "mob_drops", "Mob Drops", 2_000L);
        assertSeed(items, "bone", "mob_drops", "Mob Drops", 8_000L);
        assertSeed(items, "string", "mob_drops", "Mob Drops", 8_000L);
        assertSeed(items, "gunpowder", "mob_drops", "Mob Drops", 18_000L);
        assertSeed(items, "ender_pearl", "mob_drops", "Mob Drops", 45_000L);
        assertSeed(items, "blaze_rod", "mob_drops", "Mob Drops", 60_000L);
        assertSeed(items, "ghast_tear", "mob_drops", "Mob Drops", 90_000L);
    }

    @Test
    void defaultMarketCatalog_containsRequestedFarmingItems() {
        List<MarketSeedItem> items = new DefaultMarketCatalog().items();

        assertSeed(items, "melon_slice", "farming", "Farming", 8_000L);
        assertSeed(items, "sweet_berries", "farming", "Farming", 10_000L);
        assertSeed(items, "carrot", "farming", "Farming", 10_000L);
        assertSeed(items, "potato", "farming", "Farming", 12_000L);
        assertSeed(items, "beetroot", "farming", "Farming", 14_000L);
        assertSeed(items, "pumpkin", "farming", "Farming", 12_000L);
        assertSeed(items, "apple", "farming", "Farming", 20_000L);
        assertSeed(items, "glow_berries", "farming", "Farming", 30_000L);
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
        assertEquals(81L, savedItem.getSellUnitEstimate());
    }

    @Test
    void initializeCatalogIfEmpty_updatesExistingDefaultItemCategories() {
        MarketItem diamond = pressureItem();
        diamond.setItemId("diamond");
        diamond.setCategoryId("mining");
        diamond.setCategoryDisplayName("Mining");
        diamond.setDisplayName("Diamond");
        diamond.setIconKey("DIAMOND");

        MarketItem beef = pressureItem();
        beef.setItemId("beef");
        beef.setCategoryId("farming");
        beef.setCategoryDisplayName("Farming");
        beef.setDisplayName("Raw Beef");
        beef.setIconKey("BEEF");

        when(marketItemRepository.count()).thenReturn(2L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(diamond, beef)
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

        MarketItem savedDiamond = savedItems
            .stream()
            .filter(item -> item.getItemId().equals("diamond"))
            .findFirst()
            .orElseThrow();
        assertEquals("minerals", savedDiamond.getCategoryId());
        assertEquals("Minerals", savedDiamond.getCategoryDisplayName());

        MarketItem savedBeef = savedItems
            .stream()
            .filter(item -> item.getItemId().equals("beef"))
            .findFirst()
            .orElseThrow();
        assertEquals("animal_products", savedBeef.getCategoryId());
        assertEquals("Animal Products", savedBeef.getCategoryDisplayName());
    }

    @Test
    void initializeCatalogIfEmpty_deletesRetiredDefaultItems() {
        MarketItem shulkerShell = pressureItem();
        shulkerShell.setItemId("shulker_shell");
        shulkerShell.setCategoryId("mob_drops");
        shulkerShell.setCategoryDisplayName("Mob Drops");
        shulkerShell.setDisplayName("Shulker Shell");
        shulkerShell.setIconKey("SHULKER_SHELL");

        when(marketItemRepository.count()).thenReturn(1L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(shulkerShell)
        );

        MarketCatalogInitializer initializer = new MarketCatalogInitializer(
            marketItemRepository,
            new DefaultMarketCatalog(),
            tradePlanner
        );

        initializer.initializeCatalogIfEmpty();

        verify(marketItemRepository).deleteAllById(List.of("shulker_shell"));
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
            validSeedBuilder().minUnitPrice(100L).maxUnitPrice(100L),
            validSeedBuilder().segmentSize(0L),
            seedBuilderWithoutPriceSensitivity(),
            validSeedBuilder().priceSensitivity("0.0000"),
            validSeedBuilder().sellPricePercentage("0.0000"),
            validSeedBuilder().sellPricePercentage("1.0000"),
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
            .sellPricePercentage("0.7000")
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
        assertEquals(
            0,
            new BigDecimal("0.7000").compareTo(item.sellPricePercentage())
        );
        assertTrue(item.baseRegenQuantity() > 0L);
    }

    private static void assertCategoryOrder(
        List<MarketSeedItem> items,
        String categoryId,
        List<String> expectedItemIds
    ) {
        List<String> actualItemIds = items
            .stream()
            .filter(seed -> seed.categoryId().equals(categoryId))
            .map(MarketSeedItem::itemId)
            .toList();

        assertEquals(expectedItemIds, actualItemIds);
    }
}
