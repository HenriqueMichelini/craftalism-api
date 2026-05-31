package io.github.HenriqueMichelini.craftalism.api.config;

import static org.mockito.Mockito.verify;

import io.github.HenriqueMichelini.craftalism.api.service.MarketEventTemplateService;
import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class MarketCatalogInitializerTest {

    @Mock
    private MarketService marketService;

    @Mock
    private MarketEventTemplateService marketEventTemplateService;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void runInitializesCatalogAndSeedsMarketEventTemplates() {
        MarketCatalogInitializer initializer = new MarketCatalogInitializer(
            marketService,
            marketEventTemplateService
        );

        initializer.run(applicationArguments);

        verify(marketService).initializeCatalogIfEmpty();
        verify(marketEventTemplateService).seedInitialTemplatesIfEmpty();
    }
}
