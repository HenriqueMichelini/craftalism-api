package io.github.HenriqueMichelini.craftalism.api.service;

import java.math.BigDecimal;

record MarketSeedItem(
    String itemId,
    String categoryId,
    String categoryDisplayName,
    String displayName,
    String iconKey,
    BigDecimal variationPercent,
    long baseUnitPrice,
    long minUnitPrice,
    long maxUnitPrice,
    long segmentSize,
    BigDecimal priceSensitivity,
    long baseRegenQuantity,
    long regenIntervalSeconds,
    Long minNetPosition,
    Long maxNetPosition
) {
    MarketSeedItem {
        requireText(itemId, "itemId");
        requireText(categoryId, "categoryId");
        requireText(categoryDisplayName, "categoryDisplayName");
        requireText(displayName, "displayName");
        requireText(iconKey, "iconKey");
        if (variationPercent == null) {
            throw new IllegalArgumentException(
                "variationPercent must be provided"
            );
        }
        if (baseUnitPrice <= 0L) {
            throw new IllegalArgumentException(
                "baseUnitPrice must be positive"
            );
        }
        if (minUnitPrice <= 0L) {
            throw new IllegalArgumentException(
                "minUnitPrice must be positive"
            );
        }
        if (minUnitPrice > baseUnitPrice) {
            throw new IllegalArgumentException(
                "minUnitPrice must not exceed baseUnitPrice"
            );
        }
        if (maxUnitPrice < baseUnitPrice) {
            throw new IllegalArgumentException(
                "maxUnitPrice must not be below baseUnitPrice"
            );
        }
        if (segmentSize <= 0L) {
            throw new IllegalArgumentException("segmentSize must be positive");
        }
        if (priceSensitivity == null) {
            throw new IllegalArgumentException(
                "priceSensitivity must be provided"
            );
        }
        if (priceSensitivity.signum() <= 0) {
            throw new IllegalArgumentException(
                "priceSensitivity must be positive"
            );
        }
        if (baseRegenQuantity < 0L) {
            throw new IllegalArgumentException(
                "baseRegenQuantity must not be negative"
            );
        }
        if (regenIntervalSeconds <= 0L) {
            throw new IllegalArgumentException(
                "regenIntervalSeconds must be positive"
            );
        }
        if (minNetPosition != null && minNetPosition > 0L) {
            throw new IllegalArgumentException(
                "minNetPosition must be non-positive when provided"
            );
        }
        if (maxNetPosition != null && maxNetPosition < 0L) {
            throw new IllegalArgumentException(
                "maxNetPosition must be non-negative when provided"
            );
        }
        if (
            minNetPosition != null &&
            maxNetPosition != null &&
            minNetPosition > maxNetPosition
        ) {
            throw new IllegalArgumentException(
                "minNetPosition must not exceed maxNetPosition"
            );
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be provided");
        }
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

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
        private long baseRegenQuantity;
        private long regenIntervalSeconds;
        private Long minNetPosition;
        private Long maxNetPosition;

        Builder itemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        Builder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        Builder categoryDisplayName(String categoryDisplayName) {
            this.categoryDisplayName = categoryDisplayName;
            return this;
        }

        Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        Builder iconKey(String iconKey) {
            this.iconKey = iconKey;
            return this;
        }

        Builder variationPercent(String variationPercent) {
            this.variationPercent = new BigDecimal(variationPercent);
            return this;
        }

        Builder baseUnitPrice(long baseUnitPrice) {
            this.baseUnitPrice = baseUnitPrice;
            return this;
        }

        Builder minUnitPrice(long minUnitPrice) {
            this.minUnitPrice = minUnitPrice;
            return this;
        }

        Builder maxUnitPrice(long maxUnitPrice) {
            this.maxUnitPrice = maxUnitPrice;
            return this;
        }

        Builder segmentSize(long segmentSize) {
            this.segmentSize = segmentSize;
            return this;
        }

        Builder priceSensitivity(String priceSensitivity) {
            this.priceSensitivity = new BigDecimal(priceSensitivity);
            return this;
        }

        Builder baseRegenQuantity(long baseRegenQuantity) {
            this.baseRegenQuantity = baseRegenQuantity;
            return this;
        }

        Builder regenIntervalSeconds(long regenIntervalSeconds) {
            this.regenIntervalSeconds = regenIntervalSeconds;
            return this;
        }

        Builder minNetPosition(Long minNetPosition) {
            this.minNetPosition = minNetPosition;
            return this;
        }

        Builder maxNetPosition(Long maxNetPosition) {
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
                baseRegenQuantity,
                regenIntervalSeconds,
                minNetPosition,
                maxNetPosition
            );
        }
    }
}
