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
        if (item.getCurrentStock() < 0L || item.getMarketMomentum() < -1L) {
            throw new IllegalStateException(
                "Legacy market state could not be deterministically backfilled into segments."
            );
        }
        if (item.getMarketMomentum() == -1L && item.getCurrentStock() <= 0L) {
            throw new IllegalStateException(
                "Legacy market state could not be deterministically backfilled into segments."
            );
        }

        long segmentCount = legacySegmentCount(item);
        long frontierRemainingCapacity = legacyFrontierRemainingCapacity(item);
        long legacyBasePrice = legacyBasePrice(item, frontierRemainingCapacity);
        List<MarketSegment> segments = new ArrayList<>();

        for (
            long segmentIndex = 0L;
            segmentIndex < segmentCount;
            segmentIndex++
        ) {
            long capacity = legacySegmentCapacity(
                item,
                segmentIndex,
                segmentCount
            );
            MarketSegment segment = new MarketSegment();
            segment.setSegmentIndex(segmentIndex);
            segment.setMaxCapacity(capacity);
            segment.setRemainingCapacity(
                legacyRemainingCapacity(
                    item,
                    segmentIndex,
                    frontierRemainingCapacity,
                    capacity
                )
            );
            segment.setUnitPrice(Math.addExact(legacyBasePrice, segmentIndex));
            segments.add(segment);
        }

        return segments;
    }

    private long legacySegmentCount(MarketItem item) {
        if (item.getMarketMomentum() == -1L) {
            return Math.max(
                1L,
                divideRoundUp(
                    Math.max(item.getCurrentStock(), 1L),
                    LEGACY_SEGMENT_CAPACITY
                )
            );
        }
        return Math.addExact(
            Math.addExact(item.getMarketMomentum(), 1L),
            Math.floorDiv(item.getCurrentStock(), LEGACY_SEGMENT_CAPACITY)
        );
    }

    private long legacyFrontierRemainingCapacity(MarketItem item) {
        if (item.getMarketMomentum() == -1L) {
            return LEGACY_SEGMENT_CAPACITY;
        }
        return Math.floorMod(
            item.getCurrentStock(),
            LEGACY_SEGMENT_CAPACITY
        );
    }

    private long legacyBasePrice(
        MarketItem item,
        long frontierRemainingCapacity
    ) {
        long legacyBuyFrontier = legacyBuyFrontier(
            item,
            frontierRemainingCapacity
        );
        return Math.max(1L, item.getBuyUnitEstimate() - legacyBuyFrontier);
    }

    private long legacyBuyFrontier(
        MarketItem item,
        long frontierRemainingCapacity
    ) {
        if (item.getMarketMomentum() == -1L) {
            return 0L;
        }
        if (item.getCurrentStock() == 0L || frontierRemainingCapacity > 0L) {
            return item.getMarketMomentum();
        }
        return Math.addExact(item.getMarketMomentum(), 1L);
    }

    private long legacySegmentCapacity(
        MarketItem item,
        long segmentIndex,
        long segmentCount
    ) {
        if (
            item.getMarketMomentum() == -1L &&
            segmentIndex == segmentCount - 1L &&
            item.getCurrentStock() > 0L
        ) {
            long remainder = Math.floorMod(
                item.getCurrentStock(),
                LEGACY_SEGMENT_CAPACITY
            );
            if (remainder > 0L) {
                return remainder;
            }
        }
        return LEGACY_SEGMENT_CAPACITY;
    }

    private long legacyRemainingCapacity(
        MarketItem item,
        long segmentIndex,
        long frontierRemainingCapacity,
        long capacity
    ) {
        if (item.getMarketMomentum() == -1L) {
            return capacity;
        }
        if (segmentIndex < item.getMarketMomentum()) {
            return 0L;
        }
        if (segmentIndex == item.getMarketMomentum()) {
            return frontierRemainingCapacity;
        }
        return capacity;
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
        item.setBaseUnitPrice(seed.baseUnitPrice());
        item.setMinUnitPrice(seed.minUnitPrice());
        item.setMaxUnitPrice(seed.maxUnitPrice());
        item.setSegmentSize(seed.segmentSize());
        item.setPriceSensitivity(seed.priceSensitivity());
        item.setBaseRegenQuantity(seed.baseRegenQuantity());
        item.setRegenIntervalSeconds(seed.regenIntervalSeconds());
        item.setNetPosition(0L);
        item.setMinNetPosition(seed.minNetPosition());
        item.setMaxNetPosition(seed.maxNetPosition());
        item.setVariationPercent(seed.variationPercent());
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.now());
        item.setSegments(
            explicitSeedSegments(seed.baseUnitPrice(), legacySegmentCount(seed))
        );
        tradePlanner.recomputeDerivedProjections(item);
        return item;
    }

    private int legacySegmentCount(MarketSeedItem seed) {
        return switch (seed.itemId()) {
            case "wheat" -> 37;
            case "carrot" -> 29;
            case "iron_ingot" -> 13;
            default -> throw new IllegalArgumentException(
                "No legacy segment count configured for " + seed.itemId()
            );
        };
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
