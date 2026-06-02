package io.github.HenriqueMichelini.craftalism.api.market.application.query;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.market.domain.pricing.MarketDriftService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlanner;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class MarketSnapshotStateLoader {

    private final MarketItemRepository marketItemRepository;
    private final MarketTradePlanner tradePlanner;
    private final MarketDriftService driftService;
    private final Clock clock;

    public MarketSnapshotStateLoader(
        MarketItemRepository marketItemRepository,
        MarketTradePlanner tradePlanner
    ) {
        this(
            marketItemRepository,
            tradePlanner,
            new MarketDriftService(),
            Clock.systemUTC()
        );
    }

    MarketSnapshotStateLoader(
        MarketItemRepository marketItemRepository,
        MarketTradePlanner tradePlanner,
        Clock clock
    ) {
        this(
            marketItemRepository,
            tradePlanner,
            new MarketDriftService(),
            clock
        );
    }

    MarketSnapshotStateLoader(
        MarketItemRepository marketItemRepository,
        MarketTradePlanner tradePlanner,
        MarketDriftService driftService,
        Clock clock
    ) {
        this.marketItemRepository = marketItemRepository;
        this.tradePlanner = tradePlanner;
        this.driftService = driftService;
        this.clock = clock;
    }

    public List<MarketItem> refreshedItems() {
        return regeneratedItems().items();
    }

    MarketSnapshotState regeneratedItems() {
        tradePlanner.clearPricingCache();
        try {
            return regeneratedItemsWithinPricingCache();
        } finally {
            tradePlanner.clearPricingCache();
        }
    }

    private MarketSnapshotState regeneratedItemsWithinPricingCache() {
        long fetchStartNanos = System.nanoTime();
        List<MarketItem> items = new ArrayList<>(
            marketItemRepository.findAllForMarketRead()
        );
        long fetchNanos = System.nanoTime() - fetchStartNanos;

        long regenerationStartNanos = System.nanoTime();
        Instant now = Instant.now(clock);
        int regeneratedItemCount = 0;
        for (int index = 0; index < items.size(); index++) {
            MarketItem item = items.get(index);
            if (!shouldAttemptMarketStateUpdate(item, now)) {
                continue;
            }
            MarketItem lockedItem = marketItemRepository
                .findForUpdate(item.getItemId())
                .orElse(null);
            if (lockedItem != null && updateMarketState(lockedItem, now)) {
                regeneratedItemCount++;
                marketItemRepository.save(lockedItem);
                items.set(index, lockedItem);
            }
        }
        long regenerationNanos = System.nanoTime() - regenerationStartNanos;
        return new MarketSnapshotState(
            List.copyOf(items),
            fetchNanos,
            regenerationNanos,
            regeneratedItemCount
        );
    }

    private boolean shouldAttemptRegeneration(MarketItem item, Instant now) {
        if (item.getNetPosition() == 0L || !now.isAfter(item.getLastUpdatedAt())) {
            return false;
        }

        long ticks =
            Duration.between(item.getLastUpdatedAt(), now).getSeconds() /
            item.getRegenIntervalSeconds();
        return ticks > 0L;
    }

    private boolean shouldAttemptMarketStateUpdate(
        MarketItem item,
        Instant now
    ) {
        return (
            shouldAttemptRegeneration(item, now) ||
            driftService.shouldAttemptDriftEvaluation(item, now)
        );
    }

    private boolean updateMarketState(MarketItem item, Instant now) {
        boolean pressureChanged = regenerateItem(item, now);
        boolean driftChanged = driftService.evaluateDrift(item, now);
        if (pressureChanged || driftChanged) {
            tradePlanner.recomputeDerivedProjections(item);
            return true;
        }
        return false;
    }

    private boolean regenerateItem(MarketItem item, Instant now) {
        tradePlanner.recomputeDerivedProjections(item);
        if (item.getNetPosition() == 0L || !now.isAfter(item.getLastUpdatedAt())) {
            return false;
        }

        long ticks =
            Duration.between(item.getLastUpdatedAt(), now).getSeconds() /
            item.getRegenIntervalSeconds();
        if (ticks <= 0L) {
            return false;
        }

        long regenQuantity = Math.multiplyExact(
            ticks,
            item.getBaseRegenQuantity()
        );
        if (regenQuantity <= 0L) {
            return false;
        }

        long netPosition = item.getNetPosition();
        if (netPosition > 0L) {
            item.setNetPosition(Math.max(0L, netPosition - regenQuantity));
        } else {
            item.setNetPosition(Math.min(0L, netPosition + regenQuantity));
        }
        item.setLastUpdatedAt(
            item
                .getLastUpdatedAt()
                .plusSeconds(
                    Math.multiplyExact(ticks, item.getRegenIntervalSeconds())
                )
        );
        tradePlanner.recomputeDerivedProjections(item);
        return true;
    }

    record MarketSnapshotState(
        List<MarketItem> items,
        long fetchNanos,
        long regenerationNanos,
        int regeneratedItemCount
    ) {}
}
