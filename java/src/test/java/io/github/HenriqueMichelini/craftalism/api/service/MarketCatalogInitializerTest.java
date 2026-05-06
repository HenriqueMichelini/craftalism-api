package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
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
        assertEquals(37, wheat.getSegments().size());
    }

    @ParameterizedTest
    @MethodSource("invalidPressureSeeds")
    void marketSeedItem_rejectsInvalidPressureConfiguration(
        MarketSeedItem.Builder seedBuilder
    ) {
        assertThrows(IllegalArgumentException.class, seedBuilder::build);
    }

    @Test
    void initializeCatalogIfEmpty_backfillsMissingSegmentsFromLegacyState() {
        MarketItem item = legacyItem();
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
        List<MarketSegment> segments = savedItem.getSegments();

        assertEquals(80L, savedItem.getCurrentStock());
        assertEquals(20L, savedItem.getMarketMomentum());
        assertEquals(22, segments.size());
        assertEquals(0L, segments.get(19).getRemainingCapacity());
        assertEquals(30L, segments.get(20).getRemainingCapacity());
        assertEquals(25L, segments.get(20).getUnitPrice());
        assertEquals(50L, segments.get(21).getRemainingCapacity());
        assertEquals(26L, segments.get(21).getUnitPrice());
    }

    private MarketItem legacyItem() {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setBuyUnitEstimate(25L);
        item.setSellUnitEstimate(25L);
        item.setCurrency("coins");
        item.setCurrentStock(80L);
        item.setMarketMomentum(20L);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }

    private static List<MarketSeedItem.Builder> invalidPressureSeeds() {
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

    private static MarketSeedItem.Builder seedBuilderWithoutPriceSensitivity() {
        return MarketSeedItem
            .builder()
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

    private static MarketSeedItem.Builder validSeedBuilder() {
        return MarketSeedItem
            .builder()
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
}
