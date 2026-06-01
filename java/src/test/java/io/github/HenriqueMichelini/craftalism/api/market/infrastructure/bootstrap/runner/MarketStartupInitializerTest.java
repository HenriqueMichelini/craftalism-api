package io.github.HenriqueMichelini.craftalism.api.market.infrastructure.bootstrap.runner;

import static org.mockito.Mockito.verify;

import io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateService;
import io.github.HenriqueMichelini.craftalism.api.market.application.MarketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class MarketStartupInitializerTest {

    @Mock
    private MarketService marketService;

    @Mock
    private MarketEventTemplateService marketEventTemplateService;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void runInitializesCatalogAndSeedsMarketEventTemplates() {
        MarketStartupInitializer initializer = new MarketStartupInitializer(
            marketService,
            marketEventTemplateService
        );

        initializer.run(applicationArguments);

        verify(marketService).initializeCatalogIfEmpty();
        verify(marketEventTemplateService).seedInitialTemplatesIfEmpty();
    }
}
