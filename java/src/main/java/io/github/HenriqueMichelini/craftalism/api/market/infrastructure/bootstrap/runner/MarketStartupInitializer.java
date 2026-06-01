package io.github.HenriqueMichelini.craftalism.api.market.infrastructure.bootstrap.runner;

import io.github.HenriqueMichelini.craftalism.api.market.application.MarketService;
import io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MarketStartupInitializer implements ApplicationRunner {

    private final MarketService marketService;
    private final MarketEventTemplateService marketEventTemplateService;

    public MarketStartupInitializer(
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
