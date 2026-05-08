package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class MarketQuoteService {

    private final MarketSnapshotService marketSnapshotService;
    private final MarketQuoteStore quoteStore;
    private final MarketTradePlanner tradePlanner;
    private final MarketPlayerResolver playerResolver;
    private final MarketRateLimiter quoteRateLimiter;
    private final boolean marketEnabled;
    private final long quoteTtlSeconds;

    MarketQuoteService(
        MarketSnapshotService marketSnapshotService,
        MarketQuoteStore quoteStore,
        MarketTradePlanner tradePlanner,
        MarketPlayerResolver playerResolver,
        MarketRateLimiter quoteRateLimiter,
        boolean marketEnabled,
        long quoteTtlSeconds
    ) {
        this.marketSnapshotService = marketSnapshotService;
        this.quoteStore = quoteStore;
        this.tradePlanner = tradePlanner;
        this.playerResolver = playerResolver;
        this.quoteRateLimiter = quoteRateLimiter;
        this.marketEnabled = marketEnabled;
        this.quoteTtlSeconds = quoteTtlSeconds;
    }

    MarketQuoteResponseDTO quote(
        JwtAuthenticationToken authentication,
        MarketQuoteRequestDTO request,
        String playerUuidHeader
    ) {
        ensureMarketOpen();

        MarketSnapshotService.CurrentSnapshot currentSnapshot =
            marketSnapshotService.currentSnapshot();
        String currentSnapshotVersion = currentSnapshot.snapshotVersion();
        validateQuantity(request.quantity(), currentSnapshotVersion);

        UUID playerUuid = playerResolver.resolvePlayerUuid(
            authentication,
            request.playerUuid(),
            playerUuidHeader,
            marketSnapshotService::currentSnapshotVersion
        );
        enforceRateLimit(playerUuid, currentSnapshotVersion);
        if (!currentSnapshotVersion.equals(request.snapshotVersion())) {
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Snapshot is no longer current.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion
            );
        }

        MarketItem item = currentSnapshot
            .items()
            .stream()
            .filter(candidate -> candidate.getItemId().equals(request.itemId()))
            .findFirst()
            .orElseThrow(() ->
                rejection(
                    MarketRejectionCode.UNKNOWN_ITEM,
                    "Market item does not exist.",
                    HttpStatus.NOT_FOUND,
                    currentSnapshotVersion
                )
            );

        validateItemAvailability(item, currentSnapshotVersion);
        MarketTradePlanner.TradePlan plan =
            request.side() == MarketSide.BUY
                ? requireFullBuyPlan(
                      item,
                      request.quantity(),
                      currentSnapshotVersion
                  )
                : requireFullSellPlan(
                      item,
                      request.quantity(),
                      currentSnapshotVersion
                  );

        Instant expiresAt = Instant.now().plusSeconds(quoteTtlSeconds);
        String quoteToken = UUID.randomUUID().toString();

        quoteStore.put(
            new MarketQuoteStore.StoredQuote(
                quoteToken,
                playerUuid,
                item.getItemId(),
                request.side(),
                request.quantity(),
                plan.unitPrice(),
                plan.totalPrice(),
                currentSnapshotVersion,
                expiresAt,
                MarketQuote.Status.ACTIVE
            )
        );

        return new MarketQuoteResponseDTO(
            item.getItemId(),
            request.side(),
            request.quantity(),
            Long.toString(plan.unitPrice()),
            Long.toString(plan.totalPrice()),
            item.getCurrency(),
            quoteToken,
            currentSnapshotVersion,
            expiresAt,
            item.isBlocked(),
            item.isOperating()
        );
    }

    private MarketTradePlanner.TradePlan requireFullBuyPlan(
        MarketItem item,
        long requestedQuantity,
        String snapshotVersion
    ) {
        MarketTradePlanner.TradePlan plan = tradePlanner.buyPlan(
            item,
            requestedQuantity
        );
        if (plan.executedQuantity() != requestedQuantity) {
            throw rejection(
                MarketRejectionCode.INSUFFICIENT_STOCK,
                "Requested quantity exceeds configured pressure bounds.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                snapshotVersion
            );
        }
        return plan;
    }

    private MarketTradePlanner.TradePlan requireFullSellPlan(
        MarketItem item,
        long requestedQuantity,
        String snapshotVersion
    ) {
        MarketTradePlanner.TradePlan plan = tradePlanner.sellPlan(
            item,
            requestedQuantity
        );
        if (plan.executedQuantity() != requestedQuantity) {
            throw rejection(
                MarketRejectionCode.INSUFFICIENT_STOCK,
                "Requested quantity exceeds configured pressure bounds.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                snapshotVersion
            );
        }
        return plan;
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
                marketSnapshotService.currentSnapshotVersion()
            );
        }
    }

    private void enforceRateLimit(
        UUID playerUuid,
        String snapshotVersion
    ) {
        if (!quoteRateLimiter.tryAcquire(playerUuid)) {
            throw rejection(
                MarketRejectionCode.RATE_LIMITED,
                "Market request rate limit exceeded.",
                HttpStatus.TOO_MANY_REQUESTS,
                snapshotVersion
            );
        }
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
