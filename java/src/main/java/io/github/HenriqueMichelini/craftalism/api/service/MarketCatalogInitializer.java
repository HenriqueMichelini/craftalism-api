package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Instant;
import java.util.List;

final class MarketCatalogInitializer {

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

        recomputeExistingPressureProjections();
    }

    private void recomputeExistingPressureProjections() {
        List<MarketItem> items = marketItemRepository.findAllForMarketRead();
        boolean changed = false;
        for (MarketItem item : items) {
            long previousCurrentStock = item.getCurrentStock();
            long previousMarketMomentum = item.getMarketMomentum();
            long previousBuyUnitEstimate = item.getBuyUnitEstimate();
            long previousSellUnitEstimate = item.getSellUnitEstimate();

            tradePlanner.recomputeDerivedProjections(item);
            changed =
                changed ||
                previousCurrentStock != item.getCurrentStock() ||
                previousMarketMomentum != item.getMarketMomentum() ||
                previousBuyUnitEstimate != item.getBuyUnitEstimate() ||
                previousSellUnitEstimate != item.getSellUnitEstimate();
        }
        if (changed) {
            marketItemRepository.saveAll(items);
        }
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
        tradePlanner.recomputeDerivedProjections(item);
        return item;
    }
}
