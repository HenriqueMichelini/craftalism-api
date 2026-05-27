package io.github.HenriqueMichelini.craftalism.api.service;

record MarketSeedCategory(
    String categoryId,
    String displayName,
    String iconKey,
    int displayOrder
) {
    MarketSeedCategory {
        requireText(categoryId, "categoryId");
        requireText(displayName, "displayName");
        requireText(iconKey, "iconKey");
        if (displayOrder < 0) {
            throw new IllegalArgumentException(
                "displayOrder must be zero or positive"
            );
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must be provided");
        }
    }
}
