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
    int segmentCount
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
        if (segmentCount <= 0) {
            throw new IllegalArgumentException("segmentCount must be positive");
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
        private int segmentCount;

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

        Builder segmentCount(int segmentCount) {
            this.segmentCount = segmentCount;
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
                segmentCount
            );
        }
    }
}
