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

}
