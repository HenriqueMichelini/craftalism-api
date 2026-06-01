package io.github.HenriqueMichelini.craftalism.api.market.application;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.market.infrastructure.bootstrap.MarketCatalogBootstrapper;
import io.github.HenriqueMichelini.craftalism.api.market.infrastructure.store.MarketQuoteStore;
import io.github.HenriqueMichelini.craftalism.api.market.application.command.MarketExecuteService;
import io.github.HenriqueMichelini.craftalism.api.market.application.command.MarketQuoteService;
import io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotService;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

public class MarketService {

    private final MarketQuoteStore quoteStore;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketCatalogBootstrapper catalogBootstrapper;
    private final MarketSnapshotService marketSnapshotService;
    private final MarketQuoteService marketQuoteService;
    private final MarketExecuteService marketExecuteService;

    public MarketService(
        MarketQuoteStore quoteStore,
        MarketQuoteRepository marketQuoteRepository,
        MarketCatalogBootstrapper catalogBootstrapper,
        MarketSnapshotService marketSnapshotService,
        MarketQuoteService marketQuoteService,
        MarketExecuteService marketExecuteService
    ) {
        this.quoteStore = quoteStore;
        this.marketQuoteRepository = marketQuoteRepository;
        this.catalogBootstrapper = catalogBootstrapper;
        this.marketSnapshotService = marketSnapshotService;
        this.marketQuoteService = marketQuoteService;
        this.marketExecuteService = marketExecuteService;
    }

    @Transactional
    public void initializeCatalogIfEmpty() {
        catalogBootstrapper.initializeCatalogIfEmpty();
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
