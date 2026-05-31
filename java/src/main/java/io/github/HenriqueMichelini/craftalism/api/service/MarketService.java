package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

public class MarketService {

    private final MarketQuoteStore quoteStore;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketCatalogInitializer catalogInitializer;
    private final MarketSnapshotService marketSnapshotService;
    private final MarketQuoteService marketQuoteService;
    private final MarketExecuteService marketExecuteService;

    MarketService(
        MarketQuoteStore quoteStore,
        MarketQuoteRepository marketQuoteRepository,
        MarketCatalogInitializer catalogInitializer,
        MarketSnapshotService marketSnapshotService,
        MarketQuoteService marketQuoteService,
        MarketExecuteService marketExecuteService
    ) {
        this.quoteStore = quoteStore;
        this.marketQuoteRepository = marketQuoteRepository;
        this.catalogInitializer = catalogInitializer;
        this.marketSnapshotService = marketSnapshotService;
        this.marketQuoteService = marketQuoteService;
        this.marketExecuteService = marketExecuteService;
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
