package io.github.HenriqueMichelini.craftalism.api.config;

import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MarketCatalogInitializer implements ApplicationRunner {

    private final MarketService marketService;
    private final MarketEventTemplateService marketEventTemplateService;

    public MarketCatalogInitializer(
        MarketService marketService,
        MarketEventTemplateService marketEventTemplateService
    ) {
        this.marketService = marketService;
        this.marketEventTemplateService = marketEventTemplateService;
    }

    @Override
    public void run(ApplicationArguments args) {
        marketService.initializeCatalogIfEmpty();
        marketEventTemplateService.seedInitialTemplatesIfEmpty();
    }
}
