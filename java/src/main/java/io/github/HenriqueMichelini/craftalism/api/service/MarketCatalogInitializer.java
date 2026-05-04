package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class MarketCatalogInitializer {

    private static final long LEGACY_SEGMENT_CAPACITY = 50L;

    private final MarketItemRepository marketItemRepository;
    private final DefaultMarketCatalog defaultMarketCatalog;
    private final MarketTradePlanner tradePlanner;

    MarketCatalogInitializer(
        MarketItemRepository marketItemRepository,
        DefaultMarketCatalog defaultMarketCatalog,
        MarketTradePlanner tradePlanner
    ) {
        this.marketItemRepository = marketItemRepository;
        this.defaultMarketCatalog = defaultMarketCatalog;
        this.tradePlanner = tradePlanner;
    }

    void initializeCatalogIfEmpty() {
        if (marketItemRepository.count() == 0) {
            marketItemRepository.saveAll(
                defaultMarketCatalog
                    .items()
                    .stream()
                    .map(this::seedItem)
                    .toList()
            );
            return;
        }

        backfillMissingSegments();
    }

    private void backfillMissingSegments() {
        List<MarketItem> items = marketItemRepository.findAllForMarketRead();
        boolean changed = false;
        for (MarketItem item : items) {
            if (!item.getSegments().isEmpty()) {
                tradePlanner.recomputeDerivedProjections(item);
                continue;
            }
            item.setSegments(legacyBackfillSegments(item));
            tradePlanner.recomputeDerivedProjections(item);
            changed = true;
        }
        if (changed) {
            marketItemRepository.saveAll(items);
        }
    }

    private List<MarketSegment> legacyBackfillSegments(MarketItem item) {
        long consumedQuantity =
            item.getMarketMomentum() < 0L ? 0L : item.getMarketMomentum();
        long totalCapacity = Math.addExact(
            item.getCurrentStock(),
            consumedQuantity
        );
        long segmentCount = Math.max(
            1L,
            divideRoundUp(Math.max(totalCapacity, 1L), LEGACY_SEGMENT_CAPACITY)
        );
        long legacyBasePrice = Math.max(
            1L,
            item.getBuyUnitEstimate() -
                Math.floorDiv(consumedQuantity, LEGACY_SEGMENT_CAPACITY)
        );
        long remainingConsumed = consumedQuantity;
        long remainingCapacityBudget = totalCapacity;
        List<MarketSegment> segments = new ArrayList<>();

        for (
            long segmentIndex = 0L;
            segmentIndex < segmentCount;
            segmentIndex++
        ) {
            long capacity = Math.min(
                LEGACY_SEGMENT_CAPACITY,
                Math.max(remainingCapacityBudget, 1L)
            );
            long consumedInSegment = Math.min(capacity, remainingConsumed);
            MarketSegment segment = new MarketSegment();
            segment.setSegmentIndex(segmentIndex);
            segment.setMaxCapacity(capacity);
            segment.setRemainingCapacity(capacity - consumedInSegment);
            segment.setUnitPrice(Math.addExact(legacyBasePrice, segmentIndex));
            segments.add(segment);
            remainingConsumed -= consumedInSegment;
            remainingCapacityBudget = Math.max(
                0L,
                remainingCapacityBudget - capacity
            );
        }

        if (remainingConsumed != 0L) {
            throw new IllegalStateException(
                "Legacy market state could not be deterministically backfilled into segments."
            );
        }

        return segments;
    }

    private long divideRoundUp(long numerator, long denominator) {
        return Math.floorDiv(
            Math.addExact(numerator, denominator - 1L),
            denominator
        );
    }

    private MarketItem seedItem(MarketSeedItem seed) {
        MarketItem item = new MarketItem();
        item.setItemId(seed.itemId());
        item.setCategoryId(seed.categoryId());
        item.setCategoryDisplayName(seed.categoryDisplayName());
        item.setDisplayName(seed.displayName());
        item.setIconKey(seed.iconKey());
        item.setCurrency("coins");
        item.setVariationPercent(seed.variationPercent());
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.now());
        item.setSegments(
            explicitSeedSegments(seed.baseUnitPrice(), seed.segmentCount())
        );
        tradePlanner.recomputeDerivedProjections(item);
        return item;
    }

    private List<MarketSegment> explicitSeedSegments(
        long baseUnitPrice,
        int segmentCount
    ) {
        List<MarketSegment> segments = new ArrayList<>();
        for (int index = 0; index < segmentCount; index++) {
            MarketSegment segment = new MarketSegment();
            segment.setSegmentIndex(index);
            segment.setMaxCapacity(LEGACY_SEGMENT_CAPACITY);
            segment.setRemainingCapacity(LEGACY_SEGMENT_CAPACITY);
            segment.setUnitPrice(baseUnitPrice + index);
            segments.add(segment);
        }
        return segments;
    }
}
