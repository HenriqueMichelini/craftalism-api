package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketCategoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class MarketCatalogInitializer {

    private final MarketItemRepository marketItemRepository;
    private final MarketCategoryRepository marketCategoryRepository;
    private final DefaultMarketCatalog defaultMarketCatalog;
    private final MarketTradePlanner tradePlanner;

    MarketCatalogInitializer(
        MarketItemRepository marketItemRepository,
        MarketCategoryRepository marketCategoryRepository,
        DefaultMarketCatalog defaultMarketCatalog,
        MarketTradePlanner tradePlanner
    ) {
        this.marketItemRepository = marketItemRepository;
        this.marketCategoryRepository = marketCategoryRepository;
        this.defaultMarketCatalog = defaultMarketCatalog;
        this.tradePlanner = tradePlanner;
    }

    void initializeCatalogIfEmpty() {
        initializeCategories();
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

        updateExistingCatalog();
    }

    private void initializeCategories() {
        Set<String> existingCategoryIds = marketCategoryRepository
            .findAll()
            .stream()
            .map(MarketCategory::getCategoryId)
            .collect(Collectors.toSet());
        List<MarketCategory> categoriesToSave = new ArrayList<>();
        Instant now = Instant.now();
        for (MarketSeedCategory seed : defaultMarketCatalog.categories()) {
            if (!existingCategoryIds.contains(seed.categoryId())) {
                MarketCategory category = new MarketCategory();
                category.setCategoryId(seed.categoryId());
                category.setDisplayName(seed.displayName());
                category.setIconKey(seed.iconKey());
                category.setDisplayOrder(seed.displayOrder());
                category.setCreatedAt(now);
                category.setUpdatedAt(now);
                categoriesToSave.add(category);
            }
        }
        if (!categoriesToSave.isEmpty()) {
            marketCategoryRepository.saveAll(categoriesToSave);
        }
    }

    private void updateExistingCatalog() {
        List<MarketItem> items = marketItemRepository.findAllForMarketRead();
        Map<String, MarketSeedItem> defaultItemsById = defaultMarketCatalog
            .items()
            .stream()
            .collect(
                Collectors.toMap(MarketSeedItem::itemId, Function.identity())
            );
        Set<String> retiredItemIds = defaultMarketCatalog.retiredItemIds();
        Set<String> existingItemIds = new HashSet<>();
        List<MarketItem> itemsToSave = new ArrayList<>();
        List<String> itemIdsToDelete = new ArrayList<>();
        for (MarketItem item : items) {
            if (retiredItemIds.contains(item.getItemId())) {
                itemIdsToDelete.add(item.getItemId());
                continue;
            }

            existingItemIds.add(item.getItemId());
            MarketSeedItem defaultItem = defaultItemsById.get(item.getItemId());
            long previousCurrentStock = item.getCurrentStock();
            long previousMarketMomentum = item.getMarketMomentum();
            long previousBuyUnitEstimate = item.getBuyUnitEstimate();
            long previousSellUnitEstimate = item.getSellUnitEstimate();

            tradePlanner.recomputeDerivedProjections(item);
            boolean itemChanged =
                updateDefaultCategory(item, defaultItem) ||
                previousCurrentStock != item.getCurrentStock() ||
                previousMarketMomentum != item.getMarketMomentum() ||
                previousBuyUnitEstimate != item.getBuyUnitEstimate() ||
                previousSellUnitEstimate != item.getSellUnitEstimate();
            if (itemChanged) {
                itemsToSave.add(item);
            }
        }

        for (MarketSeedItem seed : defaultMarketCatalog.items()) {
            if (!existingItemIds.contains(seed.itemId())) {
                itemsToSave.add(seedItem(seed));
            }
        }

        if (!itemsToSave.isEmpty()) {
            marketItemRepository.saveAll(itemsToSave);
        }
        if (!itemIdsToDelete.isEmpty()) {
            marketItemRepository.deleteAllById(itemIdsToDelete);
        }
    }

    private boolean updateDefaultCategory(
        MarketItem item,
        MarketSeedItem defaultItem
    ) {
        if (defaultItem == null) {
            return false;
        }

        boolean changed = false;
        if (!item.getCategoryId().equals(defaultItem.categoryId())) {
            item.setCategoryId(defaultItem.categoryId());
            changed = true;
        }
        return changed;
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
        item.setSellPricePercentage(seed.sellPricePercentage());
        item.setBaseRegenQuantity(seed.baseRegenQuantity());
        item.setRegenIntervalSeconds(seed.regenIntervalSeconds());
        item.setNetPosition(0L);
        item.setMinNetPosition(seed.minNetPosition());
        item.setMaxNetPosition(seed.maxNetPosition());
        item.setVariationPercent(seed.variationPercent());
        item.setBlocked(false);
        item.setOperating(true);
        Instant now = Instant.now();
        item.setLastUpdatedAt(now);
        item.setDriftMultiplierBasisPoints(10_000L);
        item.setDriftRevision(0L);
        item.setDriftEvaluatedAt(now);
        tradePlanner.recomputeDerivedProjections(item);
        return item;
    }
}
