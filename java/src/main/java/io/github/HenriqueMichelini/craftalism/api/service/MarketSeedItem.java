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
}
