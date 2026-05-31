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
    private final MarketTradeRequestPolicy requestPolicy;
    private final long quoteTtlSeconds;

    MarketQuoteService(
        MarketSnapshotService marketSnapshotService,
        MarketQuoteStore quoteStore,
        MarketTradePlanner tradePlanner,
        MarketPlayerResolver playerResolver,
        MarketTradeRequestPolicy requestPolicy,
        long quoteTtlSeconds
    ) {
        this.marketSnapshotService = marketSnapshotService;
        this.quoteStore = quoteStore;
        this.tradePlanner = tradePlanner;
        this.playerResolver = playerResolver;
        this.requestPolicy = requestPolicy;
        this.quoteTtlSeconds = quoteTtlSeconds;
    }

    MarketQuoteResponseDTO quote(
        JwtAuthenticationToken authentication,
        MarketQuoteRequestDTO request,
        String playerUuidHeader
    ) {
        requestPolicy.ensureMarketOpen();

        MarketSnapshotService.CurrentSnapshot currentSnapshot =
            marketSnapshotService.currentSnapshot();
        String currentSnapshotVersion = currentSnapshot.snapshotVersion();
        requestPolicy.validateQuantity(request.quantity(), currentSnapshotVersion);

        UUID playerUuid = playerResolver.resolvePlayerUuid(
            authentication,
            request.playerUuid(),
            playerUuidHeader,
            marketSnapshotService::currentSnapshotVersion
        );
        requestPolicy.enforceRateLimit(playerUuid, currentSnapshotVersion);
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

        requestPolicy.validateItemAvailability(item, currentSnapshotVersion);
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
                1,
                item.getNetPosition(),
                plan.driftRevision(),
                plan.namedEventInstanceId(),
                plan.eventEffectVersion(),
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
            requestPolicy.isEffectivelyBlocked(item),
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

    private MarketRejectionException rejection(
        MarketRejectionCode code,
        String message,
        HttpStatus status,
        String snapshotVersion
    ) {
        return requestPolicy.rejection(code, message, status, snapshotVersion);
    }
}
