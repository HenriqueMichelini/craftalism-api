package io.github.HenriqueMichelini.craftalism.api.market.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventPricingService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlanner;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
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
class MarketSnapshotStateLoaderTest {

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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        assertEquals(1, snapshotState.regeneratedItemCount());
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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        assertEquals(1, snapshotState.regeneratedItemCount());
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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        assertEquals(0, snapshotState.regeneratedItemCount());
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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        assertEquals(1, snapshotState.regeneratedItemCount());
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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        long saturatedCount = items
            .stream()
            .filter(item ->
                item.getDriftMultiplierBasisPoints() == 9_400L ||
                item.getDriftMultiplierBasisPoints() == 10_600L
            )
            .count();
        assertEquals(72, snapshotState.regeneratedItemCount());
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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        assertEquals(0, snapshotState.regeneratedItemCount());
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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        assertEquals(1, snapshotState.regeneratedItemCount());
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

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState = service()
            .regeneratedItems();

        assertEquals(1, snapshotState.regeneratedItemCount());
        assertEquals(0L, item.getNetPosition());
        assertEquals(NOW, item.getLastUpdatedAt());
        verify(marketItemRepository).save(item);
    }

    @Test
    void regeneratedItems_clearsEventPricingCacheBetweenOperations() {
        MarketItem duringEvent = pressureItem(1L, NOW.minusSeconds(60L));
        MarketItem afterEvent = pressureItem(1L, NOW.minusSeconds(60L));
        MarketEventInstanceRepository eventRepository = mock(
            MarketEventInstanceRepository.class
        );
        when(eventRepository.findEffectiveActive(any()))
            .thenReturn(Optional.of(categoryEvent(12_000)))
            .thenReturn(Optional.empty());
        MarketTradePlanner eventAwarePlanner = new MarketTradePlanner(
            new MarketEventPricingService(
                new MarketEventLifecycleService(eventRepository)
            )
        );
        MarketSnapshotStateLoader eventAwareService =
            new MarketSnapshotStateLoader(
                marketItemRepository,
                eventAwarePlanner,
                clock
            );
        when(marketItemRepository.findAllForMarketRead())
            .thenReturn(List.of(duringEvent))
            .thenReturn(List.of(afterEvent));
        when(marketItemRepository.findForUpdate("wheat"))
            .thenReturn(Optional.of(duringEvent))
            .thenReturn(Optional.of(afterEvent));

        eventAwareService.regeneratedItems();
        eventAwareService.regeneratedItems();

        assertEquals(120L, duringEvent.getBuyUnitEstimate());
        assertEquals(100L, afterEvent.getBuyUnitEstimate());
    }

    private MarketSnapshotStateLoader service() {
        return new MarketSnapshotStateLoader(
            marketItemRepository,
            tradePlanner,
            clock
        );
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

    private MarketEventInstance categoryEvent(int effectBasisPoints) {
        MarketEventInstance event = new MarketEventInstance();
        event.setId(42L);
        event.setTemplateId("farming_bumper_crop");
        event.setSource(MarketEventSource.SCHEDULER);
        event.setRarity(MarketEventRarity.MEDIUM);
        event.setScope(MarketEventScope.CATEGORY);
        event.setSelectedCategoryId("farming");
        event.setEffectBasisPoints(effectBasisPoints);
        event.setEffectVersion(1);
        event.setBlocking(false);
        event.setStartedAt(NOW.minusSeconds(60L));
        event.setEndsAt(NOW.plusSeconds(60L));
        event.setStatus(MarketEventStatus.ACTIVE);
        event.setCreatedAt(NOW.minusSeconds(60L));
        event.setUpdatedAt(NOW.minusSeconds(60L));
        return event;
    }
}
