package io.github.HenriqueMichelini.craftalism.api.market.infrastructure.configuration;

import io.github.HenriqueMichelini.craftalism.api.config.MarketSettings;
import io.github.HenriqueMichelini.craftalism.api.market.application.MarketService;
import io.github.HenriqueMichelini.craftalism.api.market.application.command.MarketExecuteService;
import io.github.HenriqueMichelini.craftalism.api.market.application.command.MarketPlayerResolver;
import io.github.HenriqueMichelini.craftalism.api.market.application.command.MarketQuoteService;
import io.github.HenriqueMichelini.craftalism.api.market.application.command.MarketTradeExecutor;
import io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketEventPublicContextService;
import io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotStateLoader;
import io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.catalog.DefaultMarketCatalog;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventBlockingService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventPricingService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.rate.MarketRateLimiter;
import io.github.HenriqueMichelini.craftalism.api.market.domain.snapshot.MarketSnapshotProjector;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlanner;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradeRequestPolicy;
import io.github.HenriqueMichelini.craftalism.api.market.infrastructure.bootstrap.MarketCatalogBootstrapper;
import io.github.HenriqueMichelini.craftalism.api.market.infrastructure.store.MarketQuoteStore;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketCategoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketServiceConfiguration {

    private final Clock clock;

    public MarketServiceConfiguration() {
        this(Clock.systemUTC());
    }

    public MarketServiceConfiguration(Clock clock) {
        this.clock = clock;
    }

    @Bean
    MarketSettings marketSettings(
        @Value("${craftalism.market.enabled:true}") boolean marketEnabled,
        @Value("${craftalism.market.quote-ttl-seconds:60}") long quoteTtlSeconds,
        @Value(
            "${craftalism.market.trusted-minecraft-server-client-id:minecraft-server}"
        ) String trustedMinecraftServerClientId,
        @Value("${craftalism.market.quote-rate-limit.max-requests:0}") int quoteRateLimitMaxRequests,
        @Value("${craftalism.market.execute-rate-limit.max-requests:0}") int executeRateLimitMaxRequests,
        @Value("${craftalism.market.rate-limit.window-seconds:60}") long rateLimitWindowSeconds
    ) {
        return new MarketSettings(
            marketEnabled,
            quoteTtlSeconds,
            trustedMinecraftServerClientId,
            quoteRateLimitMaxRequests,
            executeRateLimitMaxRequests,
            rateLimitWindowSeconds
        );
    }

    @Bean
    public MarketService marketService(
        MarketItemRepository marketItemRepository,
        MarketCategoryRepository marketCategoryRepository,
        BalanceRepository balanceRepository,
        MarketQuoteStore quoteStore,
        MarketQuoteRepository marketQuoteRepository,
        MarketTradeHistoryRepository marketTradeHistoryRepository,
        MarketEventPublicContextService eventPublicContextService,
        MarketEventPricingService eventPricingService,
        MarketEventBlockingService eventBlockingService,
        DefaultMarketCatalog defaultMarketCatalog,
        MarketSettings settings
    ) {
        MarketTradePlanner tradePlanner = new MarketTradePlanner(
            eventPricingService
        );
        MarketCatalogBootstrapper catalogBootstrapper =
            new MarketCatalogBootstrapper(
                marketItemRepository,
                marketCategoryRepository,
                defaultMarketCatalog,
                tradePlanner
            );
        MarketSnapshotService marketSnapshotService =
            new MarketSnapshotService(
                new MarketSnapshotStateLoader(marketItemRepository, tradePlanner),
                new MarketSnapshotProjector(tradePlanner, eventBlockingService),
                eventPublicContextService
            );
        MarketTradeExecutor tradeExecutor = new MarketTradeExecutor(
            balanceRepository,
            marketItemRepository,
            marketTradeHistoryRepository,
            tradePlanner
        );
        Duration rateLimitWindow = Duration.ofSeconds(
            settings.rateLimitWindowSeconds()
        );
        MarketPlayerResolver playerResolver = new MarketPlayerResolver(
            settings.trustedMinecraftServerClientId()
        );
        MarketTradeRequestPolicy quoteRequestPolicy =
            new MarketTradeRequestPolicy(
                marketSnapshotService,
                new MarketRateLimiter(
                    settings.quoteRateLimitMaxRequests(),
                    rateLimitWindow,
                    clock
                ),
                eventBlockingService,
                settings.enabled()
            );
        MarketTradeRequestPolicy executeRequestPolicy =
            new MarketTradeRequestPolicy(
                marketSnapshotService,
                new MarketRateLimiter(
                    settings.executeRateLimitMaxRequests(),
                    rateLimitWindow,
                    clock
                ),
                eventBlockingService,
                settings.enabled()
            );
        MarketQuoteService marketQuoteService = new MarketQuoteService(
            marketSnapshotService,
            quoteStore,
            tradePlanner,
            playerResolver,
            quoteRequestPolicy,
            settings.quoteTtlSeconds()
        );
        MarketExecuteService marketExecuteService = new MarketExecuteService(
            marketItemRepository,
            marketSnapshotService,
            quoteStore,
            tradeExecutor,
            playerResolver,
            executeRequestPolicy
        );
        return new MarketService(
            quoteStore,
            marketQuoteRepository,
            catalogBootstrapper,
            marketSnapshotService,
            marketQuoteService,
            marketExecuteService
        );
    }
}
