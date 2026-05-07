package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketReadServiceTest {

    private static final Instant NOW = Instant.parse("2026-04-12T18:30:00Z");

    @Mock
    private MarketItemRepository marketItemRepository;

    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void regeneratedItems_decreasesPositivePressureTowardZero() {
        MarketItem item = pressureItem(10L, NOW.minusSeconds(60L));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(
            Optional.of(item)
        );

        MarketReadService.MarketReadState readState = service()
            .regeneratedItems();

        assertEquals(1, readState.regeneratedItemCount());
        assertEquals(9L, item.getNetPosition());
        assertEquals(NOW, item.getLastUpdatedAt());
        verify(marketItemRepository).save(item);
    }

    @Test
    void regeneratedItems_increasesNegativePressureTowardZero() {
        MarketItem item = pressureItem(-10L, NOW.minusSeconds(60L));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(
            Optional.of(item)
        );

        MarketReadService.MarketReadState readState = service()
            .regeneratedItems();

        assertEquals(1, readState.regeneratedItemCount());
        assertEquals(-9L, item.getNetPosition());
        assertEquals(NOW, item.getLastUpdatedAt());
        verify(marketItemRepository).save(item);
    }

    @Test
    void regeneratedItems_doesNotSaveBalancedPressure() {
        MarketItem item = pressureItem(0L, NOW.minusSeconds(60L));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );

        MarketReadService.MarketReadState readState = service()
            .regeneratedItems();

        assertEquals(0, readState.regeneratedItemCount());
        assertEquals(0L, item.getNetPosition());
        assertEquals(NOW.minusSeconds(60L), item.getLastUpdatedAt());
        verify(marketItemRepository, never()).save(item);
    }

    @Test
    void regeneratedItems_doesNotSaveWhenNoWholeTickElapsed() {
        MarketItem item = pressureItem(10L, NOW.minusSeconds(59L));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );

        MarketReadService.MarketReadState readState = service()
            .regeneratedItems();

        assertEquals(0, readState.regeneratedItemCount());
        assertEquals(10L, item.getNetPosition());
        assertEquals(NOW.minusSeconds(59L), item.getLastUpdatedAt());
        verify(marketItemRepository, never()).save(item);
    }

    @Test
    void regeneratedItems_appliesWholeTicksAndPreservesFractionalRemainder() {
        MarketItem item = pressureItem(10L, NOW.minusSeconds(125L));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(
            Optional.of(item)
        );

        MarketReadService.MarketReadState readState = service()
            .regeneratedItems();

        assertEquals(1, readState.regeneratedItemCount());
        assertEquals(8L, item.getNetPosition());
        assertEquals(NOW.minusSeconds(5L), item.getLastUpdatedAt());
        verify(marketItemRepository).save(item);
    }

    @Test
    void regeneratedItems_clampsNegativePressureAtZeroAfterMultipleTicks() {
        MarketItem item = pressureItem(-2L, NOW.minusSeconds(300L));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(
            Optional.of(item)
        );

        MarketReadService.MarketReadState readState = service()
            .regeneratedItems();

        assertEquals(1, readState.regeneratedItemCount());
        assertEquals(0L, item.getNetPosition());
        assertEquals(NOW, item.getLastUpdatedAt());
        verify(marketItemRepository).save(item);
    }

    private MarketReadService service() {
        return new MarketReadService(marketItemRepository, tradePlanner, clock);
    }

    private MarketItem pressureItem(long netPosition, Instant lastUpdatedAt) {
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
        item.setNetPosition(netPosition);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(lastUpdatedAt);
        tradePlanner.recomputeDerivedProjections(item);
        return item;
    }
}
