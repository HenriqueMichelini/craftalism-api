package io.github.HenriqueMichelini.craftalism.api.config;

import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MarketCatalogInitializer implements ApplicationRunner {

    private final MarketService marketService;

    public MarketCatalogInitializer(MarketService marketService) {
        this.marketService = marketService;
    }

    @Override
    public void run(ApplicationArguments args) {
        marketService.initializeCatalogIfEmpty();
    }
}
