package io.github.HenriqueMichelini.craftalism.api.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefaultMarketCatalog {

    private static final List<MarketSeedItem> ITEMS = List.of(
        new MarketSeedItem(
            "wheat",
            "farming",
            "Farming",
            "Wheat",
            "WHEAT",
            new BigDecimal("2.3"),
            50000L,
            37
        ),
        new MarketSeedItem(
            "carrot",
            "farming",
            "Farming",
            "Carrot",
            "CARROT",
            new BigDecimal("-1.4"),
            10000L,
            29
        ),
        new MarketSeedItem(
            "iron_ingot",
            "mining",
            "Mining",
            "Iron Ingot",
            "IRON_INGOT",
            new BigDecimal("1.1"),
            140000L,
            13
        )
    );

    public List<MarketSeedItem> items() {
        return ITEMS;
    }
}
