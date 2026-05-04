package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketReadServiceTest {

    @Mock
    private MarketItemRepository marketItemRepository;

    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();

    @Test
    void regeneratedItems_restoresSegmentsAndSavesChangedItem() {
        MarketItem item = marketItem(
            Instant.now().minusSeconds(90L),
            segment(0L, 50L, 0L, 5L),
            segment(1L, 50L, 10L, 6L)
        );
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(
            Optional.of(item)
        );

        MarketReadService service = new MarketReadService(
            marketItemRepository,
            tradePlanner
        );

        MarketReadService.MarketReadState readState =
            service.regeneratedItems();

        assertEquals(1, readState.regeneratedItemCount());
        assertEquals(1, readState.items().size());
        assertEquals(12L, item.getCurrentStock());
        assertEquals(1L, item.getMarketMomentum());
        assertEquals(0L, item.getSegments().get(0).getRemainingCapacity());
        assertEquals(12L, totalRemainingCapacity(item));
        verify(marketItemRepository).save(item);
    }

    @Test
    void regeneratedItems_doesNotSaveFullyRestoredItem() {
        MarketItem item = marketItem(
            Instant.now().minusSeconds(90L),
            segment(0L, 50L, 50L, 5L)
        );
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );

        MarketReadService service = new MarketReadService(
            marketItemRepository,
            tradePlanner
        );

        MarketReadService.MarketReadState readState =
            service.regeneratedItems();

        assertEquals(0, readState.regeneratedItemCount());
        assertEquals(50L, item.getCurrentStock());
        assertEquals(-1L, item.getMarketMomentum());
        verify(marketItemRepository, never()).save(item);
    }

    private long totalRemainingCapacity(MarketItem item) {
        long total = 0L;
        for (MarketSegment segment : item.getSegments()) {
            total += segment.getRemainingCapacity();
        }
        return total;
    }

    private MarketItem marketItem(
        Instant lastUpdatedAt,
        MarketSegment... segments
    ) {
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
        item.setLastUpdatedAt(lastUpdatedAt);
        for (MarketSegment segment : segments) {
            item.addSegment(segment);
        }
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
