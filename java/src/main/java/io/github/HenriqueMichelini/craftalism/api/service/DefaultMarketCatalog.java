package io.github.HenriqueMichelini.craftalism.api.service;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefaultMarketCatalog {

    private static final List<MarketSeedItem> ITEMS = List.of(
        MarketSeedItem
            .builder()
            .itemId("wheat")
            .categoryId("farming")
            .categoryDisplayName("Farming")
            .displayName("Wheat")
            .iconKey("WHEAT")
            .variationPercent("2.3")
            .baseUnitPrice(50000L)
            .minUnitPrice(25000L)
            .maxUnitPrice(150000L)
            .segmentSize(50L)
            .priceSensitivity("0.0800")
            .baseRegenQuantity(1L)
            .regenIntervalSeconds(60L)
            .build(),
        MarketSeedItem
            .builder()
            .itemId("carrot")
            .categoryId("farming")
            .categoryDisplayName("Farming")
            .displayName("Carrot")
            .iconKey("CARROT")
            .variationPercent("-1.4")
            .baseUnitPrice(10000L)
            .minUnitPrice(5000L)
            .maxUnitPrice(30000L)
            .segmentSize(50L)
            .priceSensitivity("0.0800")
            .baseRegenQuantity(1L)
            .regenIntervalSeconds(60L)
            .build(),
        MarketSeedItem
            .builder()
            .itemId("iron_ingot")
            .categoryId("mining")
            .categoryDisplayName("Mining")
            .displayName("Iron Ingot")
            .iconKey("IRON_INGOT")
            .variationPercent("1.1")
            .baseUnitPrice(140000L)
            .minUnitPrice(70000L)
            .maxUnitPrice(420000L)
            .segmentSize(50L)
            .priceSensitivity("0.0800")
            .baseRegenQuantity(1L)
            .regenIntervalSeconds(60L)
            .build()
    );

    public List<MarketSeedItem> items() {
        return ITEMS;
    }
}
