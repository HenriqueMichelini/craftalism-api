package io.github.HenriqueMichelini.craftalism.api.service;

import java.math.BigDecimal;

final class MarketSeedItemBuilder {

    private String itemId;
    private String categoryId;
    private String categoryDisplayName;
    private String displayName;
    private String iconKey;
    private BigDecimal variationPercent;
    private long baseUnitPrice;
    private long minUnitPrice;
    private long maxUnitPrice;
    private long segmentSize;
    private BigDecimal priceSensitivity;
    private BigDecimal sellPricePercentage;
    private long baseRegenQuantity;
    private long regenIntervalSeconds;
    private Long minNetPosition;
    private Long maxNetPosition;

    MarketSeedItemBuilder itemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    MarketSeedItemBuilder categoryId(String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    MarketSeedItemBuilder categoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
        return this;
    }

    MarketSeedItemBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    MarketSeedItemBuilder iconKey(String iconKey) {
        this.iconKey = iconKey;
        return this;
    }

    MarketSeedItemBuilder variationPercent(String variationPercent) {
        this.variationPercent = new BigDecimal(variationPercent);
        return this;
    }

    MarketSeedItemBuilder baseUnitPrice(long baseUnitPrice) {
        this.baseUnitPrice = baseUnitPrice;
        return this;
    }

    MarketSeedItemBuilder minUnitPrice(long minUnitPrice) {
        this.minUnitPrice = minUnitPrice;
        return this;
    }

    MarketSeedItemBuilder maxUnitPrice(long maxUnitPrice) {
        this.maxUnitPrice = maxUnitPrice;
        return this;
    }

    MarketSeedItemBuilder segmentSize(long segmentSize) {
        this.segmentSize = segmentSize;
        return this;
    }

    MarketSeedItemBuilder priceSensitivity(String priceSensitivity) {
        this.priceSensitivity = new BigDecimal(priceSensitivity);
        return this;
    }

    MarketSeedItemBuilder sellPricePercentage(String sellPricePercentage) {
        this.sellPricePercentage = new BigDecimal(sellPricePercentage);
        return this;
    }

    MarketSeedItemBuilder baseRegenQuantity(long baseRegenQuantity) {
        this.baseRegenQuantity = baseRegenQuantity;
        return this;
    }

    MarketSeedItemBuilder regenIntervalSeconds(long regenIntervalSeconds) {
        this.regenIntervalSeconds = regenIntervalSeconds;
        return this;
    }

    MarketSeedItemBuilder minNetPosition(Long minNetPosition) {
        this.minNetPosition = minNetPosition;
        return this;
    }

    MarketSeedItemBuilder maxNetPosition(Long maxNetPosition) {
        this.maxNetPosition = maxNetPosition;
        return this;
    }

    MarketSeedItem build() {
        return new MarketSeedItem(
            itemId,
            categoryId,
            categoryDisplayName,
            displayName,
            iconKey,
            variationPercent,
            baseUnitPrice,
            minUnitPrice,
            maxUnitPrice,
            segmentSize,
            priceSensitivity,
            sellPricePercentage,
            baseRegenQuantity,
            regenIntervalSeconds,
            minNetPosition,
            maxNetPosition
        );
    }
}
