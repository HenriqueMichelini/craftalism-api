package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.util.UUID;
import org.springframework.http.HttpStatus;

final class MarketTradeRequestPolicy {

    private final MarketSnapshotService marketSnapshotService;
    private final MarketRateLimiter rateLimiter;
    private final MarketEventBlockingService eventBlockingService;
    private final boolean marketEnabled;

    MarketTradeRequestPolicy(
        MarketSnapshotService marketSnapshotService,
        MarketRateLimiter rateLimiter,
        MarketEventBlockingService eventBlockingService,
        boolean marketEnabled
    ) {
        this.marketSnapshotService = marketSnapshotService;
        this.rateLimiter = rateLimiter;
        this.eventBlockingService = eventBlockingService;
        this.marketEnabled = marketEnabled;
    }

    void ensureMarketOpen() {
        if (!marketEnabled) {
            throw rejection(
                MarketRejectionCode.MARKET_CLOSED,
                "Market is currently closed.",
                HttpStatus.SERVICE_UNAVAILABLE,
                marketSnapshotService.currentSnapshotVersion()
            );
        }
    }

    void validateQuantity(Long quantity, String snapshotVersion) {
        if (quantity != null && quantity <= 0L) {
            throw rejection(
                MarketRejectionCode.INVALID_QUANTITY,
                "Quantity must be positive.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                snapshotVersion
            );
        }
    }

    void enforceRateLimit(UUID playerUuid, String snapshotVersion) {
        if (!rateLimiter.tryAcquire(playerUuid)) {
            throw rejection(
                MarketRejectionCode.RATE_LIMITED,
                "Market request rate limit exceeded.",
                HttpStatus.TOO_MANY_REQUESTS,
                snapshotVersion
            );
        }
    }

    void validateItemAvailability(MarketItem item, String snapshotVersion) {
        if (isEffectivelyBlocked(item)) {
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

    boolean isEffectivelyBlocked(MarketItem item) {
        return eventBlockingService == null
            ? item.isBlocked()
            : eventBlockingService.isEffectivelyBlocked(item);
    }

    MarketRejectionException rejection(
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
