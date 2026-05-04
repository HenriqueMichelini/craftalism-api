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
            .segmentCount(37)
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
            .segmentCount(29)
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
            .segmentCount(13)
            .build()
    );

    public List<MarketSeedItem> items() {
        return ITEMS;
    }
}
