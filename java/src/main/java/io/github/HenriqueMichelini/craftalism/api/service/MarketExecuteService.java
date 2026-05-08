package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class MarketExecuteService {

    private final MarketItemRepository marketItemRepository;
    private final MarketSnapshotService marketSnapshotService;
    private final MarketQuoteStore quoteStore;
    private final MarketTradeExecutor tradeExecutor;
    private final MarketPlayerResolver playerResolver;
    private final MarketRateLimiter executeRateLimiter;
    private final boolean marketEnabled;

    MarketExecuteService(
        MarketItemRepository marketItemRepository,
        MarketSnapshotService marketSnapshotService,
        MarketQuoteStore quoteStore,
        MarketTradeExecutor tradeExecutor,
        MarketPlayerResolver playerResolver,
        MarketRateLimiter executeRateLimiter,
        boolean marketEnabled
    ) {
        this.marketItemRepository = marketItemRepository;
        this.marketSnapshotService = marketSnapshotService;
        this.quoteStore = quoteStore;
        this.tradeExecutor = tradeExecutor;
        this.playerResolver = playerResolver;
        this.executeRateLimiter = executeRateLimiter;
        this.marketEnabled = marketEnabled;
    }

    MarketExecuteSuccessResponseDTO execute(
        JwtAuthenticationToken authentication,
        MarketExecuteRequestDTO request,
        String playerUuidHeader
    ) {
        ensureMarketOpen();

        String initialSnapshotVersion = currentSnapshotVersion();
        validateQuantity(request.quantity(), initialSnapshotVersion);

        UUID playerUuid = playerResolver.resolvePlayerUuid(
            authentication,
            request.playerUuid(),
            playerUuidHeader,
            this::currentSnapshotVersion
        );
        enforceRateLimit(playerUuid, initialSnapshotVersion);
        MarketQuoteStore.StoredQuote storedQuote = quoteStore
            .get(request.quoteToken())
            .orElseThrow(() ->
                rejection(
                    MarketRejectionCode.QUOTE_EXPIRED,
                    "Quote has expired.",
                    HttpStatus.CONFLICT,
                    currentSnapshotVersion()
                )
            );

        if (storedQuote.status() == MarketQuote.Status.EXPIRED) {
            throw rejection(
                MarketRejectionCode.QUOTE_EXPIRED,
                "Quote has expired.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        if (storedQuote.status() != MarketQuote.Status.ACTIVE) {
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        if (storedQuote.expiresAt().isBefore(Instant.now())) {
            quoteStore.expire(request.quoteToken());
            throw rejection(
                MarketRejectionCode.QUOTE_EXPIRED,
                "Quote has expired.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        if (
            !storedQuote.playerUuid().equals(playerUuid) ||
            !storedQuote.itemId().equals(request.itemId()) ||
            storedQuote.side() != request.side() ||
            storedQuote.quantity() != request.quantity()
        ) {
            quoteStore.invalidate(request.quoteToken());
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        if (!storedQuote.snapshotVersion().equals(request.snapshotVersion())) {
            quoteStore.invalidate(request.quoteToken());
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        String currentSnapshotVersion = currentSnapshotVersion();
        if (!currentSnapshotVersion.equals(storedQuote.snapshotVersion())) {
            quoteStore.invalidate(request.quoteToken());
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion
            );
        }

        if (!quoteStore.consume(request.quoteToken())) {
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        MarketItem item = marketItemRepository
            .findForUpdate(request.itemId())
            .orElseThrow(() ->
                rejection(
                    MarketRejectionCode.UNKNOWN_ITEM,
                    "Market item does not exist.",
                    HttpStatus.NOT_FOUND,
                    currentSnapshotVersion
                )
            );

        validateItemAvailability(item, currentSnapshotVersion);
        MarketTradeExecutor.AppliedTrade appliedTrade =
            tradeExecutor.applyTrade(
                playerUuid,
                item,
                storedQuote,
                currentSnapshotVersion,
                this::currentSnapshotVersion
            );

        return new MarketExecuteSuccessResponseDTO(
            "SUCCESS",
            item.getItemId(),
            storedQuote.side(),
            appliedTrade.executedQuantity(),
            Long.toString(appliedTrade.unitPrice()),
            Long.toString(appliedTrade.totalPrice()),
            item.getCurrency(),
            currentSnapshotVersion(),
            marketSnapshotService.toSnapshotItem(item)
        );
    }

    private void validateItemAvailability(
        MarketItem item,
        String snapshotVersion
    ) {
        if (item.isBlocked()) {
            throw rejection(
                MarketRejectionCode.ITEM_BLOCKED,
                "Item is blocked from trading.",
                HttpStatus.CONFLICT,
                snapshotVersion
            );
        }
        if (!item.isOperating()) {
            throw rejection(
                MarketRejectionCode.ITEM_NOT_OPERATING,
                "Item is not currently operating.",
                HttpStatus.CONFLICT,
                snapshotVersion
            );
        }
    }

    private void validateQuantity(Long quantity, String snapshotVersion) {
        if (quantity != null && quantity <= 0L) {
            throw rejection(
                MarketRejectionCode.INVALID_QUANTITY,
                "Quantity must be positive.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                snapshotVersion
            );
        }
    }

    private void ensureMarketOpen() {
        if (!marketEnabled) {
            throw rejection(
                MarketRejectionCode.MARKET_CLOSED,
                "Market is currently closed.",
                HttpStatus.SERVICE_UNAVAILABLE,
                currentSnapshotVersion()
            );
        }
    }

    private void enforceRateLimit(UUID playerUuid, String snapshotVersion) {
        if (!executeRateLimiter.tryAcquire(playerUuid)) {
            throw rejection(
                MarketRejectionCode.RATE_LIMITED,
                "Market request rate limit exceeded.",
                HttpStatus.TOO_MANY_REQUESTS,
                snapshotVersion
            );
        }
    }

    private String currentSnapshotVersion() {
        return marketSnapshotService.currentSnapshotVersion();
    }

    private MarketRejectionException rejection(
        MarketRejectionCode code,
        String message,
        HttpStatus status,
        String snapshotVersion
    ) {
        return new MarketRejectionException(
            code,
            message,
            status,
            snapshotVersion
        );
    }
}
