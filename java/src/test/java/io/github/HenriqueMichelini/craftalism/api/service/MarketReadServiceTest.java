package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.stream.IntStream;
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
        item.setDriftEvaluatedAt(NOW);
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
    void regeneratedItems_evaluatesDriftForBalancedPressureWithoutTouchingPressureTimestamp() {
        MarketItem item = pressureItem(0L, NOW.minusSeconds(60L));
        item.setDriftEvaluatedAt(NOW.minusSeconds(3_600L));
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
        assertEquals(NOW.minusSeconds(60L), item.getLastUpdatedAt());
        assertEquals(1L, item.getDriftRevision());
        assertEquals(NOW, item.getDriftEvaluatedAt());
        assertTrue(item.getDriftMultiplierBasisPoints() >= 9_400L);
        assertTrue(item.getDriftMultiplierBasisPoints() <= 10_600L);
        verify(marketItemRepository).save(item);
    }

    @Test
    void regeneratedItems_sixBalancedDriftTicksDoNotSaturateMostCatalogItems() {
        List<MarketItem> items = IntStream
            .range(0, 72)
            .mapToObj(index -> pressureItem("item_" + index, 0L, NOW.minusSeconds(60L)))
            .toList();
        when(marketItemRepository.findAllForMarketRead()).thenReturn(items);
        for (MarketItem item : items) {
            item.setDriftEvaluatedAt(NOW.minusSeconds(21_600L));
            when(marketItemRepository.findForUpdate(item.getItemId()))
                .thenReturn(Optional.of(item));
        }

        MarketReadService.MarketReadState readState = service()
            .regeneratedItems();

        long saturatedCount = items
            .stream()
            .filter(item ->
                item.getDriftMultiplierBasisPoints() == 9_400L ||
                item.getDriftMultiplierBasisPoints() == 10_600L
            )
            .count();
        assertEquals(72, readState.regeneratedItemCount());
        assertTrue(saturatedCount < 36L);
        assertTrue(
            items
                .stream()
                .allMatch(item ->
                    item.getDriftMultiplierBasisPoints() >= 9_400L &&
                    item.getDriftMultiplierBasisPoints() <= 10_600L
                )
        );
        assertTrue(
            items
                .stream()
                .allMatch(item -> item.getDriftRevision() == 6L)
        );
    }

    @Test
    void regeneratedItems_movesPinnedDriftBackInsideBoundsOnOrdinaryTick() {
        MarketItem item = pressureItem(0L, NOW.minusSeconds(60L));
        item.setDriftMultiplierBasisPoints(10_600L);
        item.setDriftEvaluatedAt(NOW.minusSeconds(3_600L));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(
            List.of(item)
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(
            Optional.of(item)
        );

        service().regeneratedItems();

        assertNotEquals(10_600L, item.getDriftMultiplierBasisPoints());
        assertTrue(item.getDriftMultiplierBasisPoints() < 10_600L);
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
        return pressureItem("wheat", netPosition, lastUpdatedAt);
    }

    private MarketItem pressureItem(
        String itemId,
        long netPosition,
        Instant lastUpdatedAt
    ) {
        MarketItem item = new MarketItem();
        item.setItemId(itemId);
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
        item.setDriftMultiplierBasisPoints(10_000L);
        item.setDriftRevision(0L);
        item.setDriftEvaluatedAt(NOW);
        tradePlanner.recomputeDerivedProjections(item);
        return item;
    }
}
