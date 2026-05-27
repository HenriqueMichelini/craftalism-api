package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketCategoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketService {

    private final MarketQuoteStore quoteStore;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();
    private final MarketPlayerResolver playerResolver;
    private final MarketCatalogInitializer catalogInitializer;
    private final MarketSnapshotService marketSnapshotService;
    private final MarketQuoteService marketQuoteService;
    private final MarketExecuteService marketExecuteService;

    @Autowired
    public MarketService(
        MarketItemRepository marketItemRepository,
        MarketCategoryRepository marketCategoryRepository,
        BalanceRepository balanceRepository,
        MarketQuoteStore quoteStore,
        MarketQuoteRepository marketQuoteRepository,
        MarketTradeHistoryRepository marketTradeHistoryRepository,
        DefaultMarketCatalog defaultMarketCatalog,
        @Value("${craftalism.market.enabled:true}") boolean marketEnabled,
        @Value(
            "${craftalism.market.quote-ttl-seconds:60}"
        ) long quoteTtlSeconds,
        @Value(
            "${craftalism.market.trusted-minecraft-server-client-id:minecraft-server}"
        ) String trustedMinecraftServerClientId,
        @Value("${craftalism.market.quote-rate-limit.max-requests:0}") int quoteRateLimitMaxRequests,
        @Value("${craftalism.market.execute-rate-limit.max-requests:0}") int executeRateLimitMaxRequests,
        @Value("${craftalism.market.rate-limit.window-seconds:60}") long rateLimitWindowSeconds
    ) {
        this(
            marketItemRepository,
            marketCategoryRepository,
            balanceRepository,
            quoteStore,
            marketQuoteRepository,
            marketTradeHistoryRepository,
            defaultMarketCatalog,
            marketEnabled,
            quoteTtlSeconds,
            trustedMinecraftServerClientId,
            quoteRateLimitMaxRequests,
            executeRateLimitMaxRequests,
            rateLimitWindowSeconds,
            Clock.systemUTC()
        );
    }

    MarketService(
        MarketItemRepository marketItemRepository,
        MarketCategoryRepository marketCategoryRepository,
        BalanceRepository balanceRepository,
        MarketQuoteStore quoteStore,
        MarketQuoteRepository marketQuoteRepository,
        MarketTradeHistoryRepository marketTradeHistoryRepository,
        DefaultMarketCatalog defaultMarketCatalog,
        boolean marketEnabled,
        long quoteTtlSeconds,
        String trustedMinecraftServerClientId,
        int quoteRateLimitMaxRequests,
        int executeRateLimitMaxRequests,
        long rateLimitWindowSeconds,
        Clock clock
    ) {
        this.quoteStore = quoteStore;
        this.marketQuoteRepository = marketQuoteRepository;
        this.playerResolver = new MarketPlayerResolver(
            trustedMinecraftServerClientId
        );
        this.catalogInitializer = new MarketCatalogInitializer(
            marketItemRepository,
            marketCategoryRepository,
            defaultMarketCatalog,
            tradePlanner
        );
        MarketReadService marketReadService = new MarketReadService(
            marketItemRepository,
            tradePlanner
        );
        this.marketSnapshotService = new MarketSnapshotService(
            marketReadService,
            new MarketSnapshotProjector(tradePlanner)
        );
        MarketTradeExecutor tradeExecutor = new MarketTradeExecutor(
            balanceRepository,
            marketItemRepository,
            marketTradeHistoryRepository,
            tradePlanner
        );
        Duration rateLimitWindow = Duration.ofSeconds(rateLimitWindowSeconds);
        MarketRateLimiter quoteRateLimiter = new MarketRateLimiter(
            quoteRateLimitMaxRequests,
            rateLimitWindow,
            clock
        );
        MarketRateLimiter executeRateLimiter = new MarketRateLimiter(
            executeRateLimitMaxRequests,
            rateLimitWindow,
            clock
        );
        this.marketQuoteService = new MarketQuoteService(
            marketSnapshotService,
            quoteStore,
            tradePlanner,
            playerResolver,
            quoteRateLimiter,
            marketEnabled,
            quoteTtlSeconds
        );
        this.marketExecuteService = new MarketExecuteService(
            marketItemRepository,
            marketSnapshotService,
            quoteStore,
            tradeExecutor,
            playerResolver,
            executeRateLimiter,
            marketEnabled
        );
    }

    @Transactional
    public void initializeCatalogIfEmpty() {
        catalogInitializer.initializeCatalogIfEmpty();
    }

    @Transactional
    public MarketSnapshotResponseDTO getSnapshot() {
        return marketSnapshotService.getSnapshot();
    }

    @Transactional
    public MarketQuoteResponseDTO quote(
        JwtAuthenticationToken authentication,
        MarketQuoteRequestDTO request,
        String playerUuidHeader
    ) {
        return marketQuoteService.quote(
            authentication,
            request,
            playerUuidHeader
        );
    }

    @Transactional
    public MarketExecuteSuccessResponseDTO execute(
        JwtAuthenticationToken authentication,
        MarketExecuteRequestDTO request,
        String playerUuidHeader
    ) {
        return marketExecuteService.execute(
            authentication,
            request,
            playerUuidHeader
        );
    }

    @Transactional
    public void deleteQuote(String quoteToken) {
        quoteStore.invalidate(quoteToken);
    }

    @Transactional
    public long activeQuoteCount() {
        quoteStore.expireActiveQuotes();
        return marketQuoteRepository.countByStatus(MarketQuote.Status.ACTIVE);
    }
}
