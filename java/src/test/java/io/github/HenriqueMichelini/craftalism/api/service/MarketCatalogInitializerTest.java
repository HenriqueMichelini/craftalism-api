package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketCatalogInitializerTest {

    @Mock
    private MarketItemRepository marketItemRepository;

    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();

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
}
