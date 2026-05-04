package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class MarketReadService {

    private static final long STOCK_REGEN_SPEED_SECONDS = 60L;
    private static final long BASE_STOCK_REGEN_QUANTITY = 1L;

    private final MarketItemRepository marketItemRepository;
    private final MarketTradePlanner tradePlanner;

    MarketReadService(
        MarketItemRepository marketItemRepository,
        MarketTradePlanner tradePlanner
    ) {
        this.marketItemRepository = marketItemRepository;
        this.tradePlanner = tradePlanner;
    }

    MarketReadState regeneratedItems() {
        long fetchStartNanos = System.nanoTime();
        List<MarketItem> items = new ArrayList<>(
            marketItemRepository.findAllForMarketRead()
        );
        long fetchNanos = System.nanoTime() - fetchStartNanos;

        long regenerationStartNanos = System.nanoTime();
        Instant now = Instant.now();
        int regeneratedItemCount = 0;
        for (int index = 0; index < items.size(); index++) {
            MarketItem item = items.get(index);
            if (!shouldAttemptRegeneration(item, now)) {
                continue;
            }
            MarketItem lockedItem = marketItemRepository
                .findForUpdate(item.getItemId())
                .orElse(null);
            if (lockedItem != null && regenerateItem(lockedItem, now)) {
                regeneratedItemCount++;
                marketItemRepository.save(lockedItem);
                items.set(index, lockedItem);
            }
        }
        long regenerationNanos = System.nanoTime() - regenerationStartNanos;
        return new MarketReadState(
            List.copyOf(items),
            fetchNanos,
            regenerationNanos,
            regeneratedItemCount
        );
    }

    private boolean shouldAttemptRegeneration(MarketItem item, Instant now) {
        if (
            restoreFrontier(item) == -1L ||
            !now.isAfter(item.getLastUpdatedAt())
        ) {
            return false;
        }

        long ticks =
            Duration.between(item.getLastUpdatedAt(), now).getSeconds() /
            STOCK_REGEN_SPEED_SECONDS;
        return ticks > 0L;
    }

    private long restoreFrontier(MarketItem item) {
        long restoreFrontier = -1L;
        for (MarketSegment segment : item.getSegments()) {
            if (segment.getRemainingCapacity() < segment.getMaxCapacity()) {
                restoreFrontier = Math.max(
                    restoreFrontier,
                    segment.getSegmentIndex()
                );
            }
        }
        return restoreFrontier;
    }

    private boolean regenerateItem(MarketItem item, Instant now) {
        tradePlanner.recomputeDerivedProjections(item);
        if (
            item.getMarketMomentum() == -1L ||
            !now.isAfter(item.getLastUpdatedAt())
        ) {
            return false;
        }

        long ticks =
            Duration.between(item.getLastUpdatedAt(), now).getSeconds() /
            STOCK_REGEN_SPEED_SECONDS;
        if (ticks <= 0L) {
            return false;
        }

        long regenQuantity = Math.multiplyExact(
            ticks,
            Math.addExact(
                BASE_STOCK_REGEN_QUANTITY,
                Math.max(item.getMarketMomentum(), 0L)
            )
        );
        MarketTradePlanner.TradePlan plan = tradePlanner.sellPlan(
            item,
            regenQuantity
        );
        if (plan.executedQuantity() <= 0L) {
            return false;
        }

        tradePlanner.applyRestoration(plan);
        item.setLastUpdatedAt(now);
        tradePlanner.recomputeDerivedProjections(item);
        return true;
    }

    record MarketReadState(
        List<MarketItem> items,
        long fetchNanos,
        long regenerationNanos,
        int regeneratedItemCount
    ) {}
}
