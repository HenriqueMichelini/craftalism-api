package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Duration;
import java.time.Instant;
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
        List<MarketItem> items = marketItemRepository.findAllForMarketRead();
        long fetchNanos = System.nanoTime() - fetchStartNanos;

        long regenerationStartNanos = System.nanoTime();
        Instant now = Instant.now();
        int regeneratedItemCount = 0;
        for (MarketItem item : items) {
            if (regenerateItem(item, now)) {
                regeneratedItemCount++;
                marketItemRepository.save(item);
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
