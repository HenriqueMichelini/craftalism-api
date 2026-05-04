package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MarketService {

    private static final long STOCK_REGEN_SPEED_SECONDS = 60L;
    private static final long BASE_STOCK_REGEN_QUANTITY = 1L;

    private final MarketItemRepository marketItemRepository;
    private final BalanceRepository balanceRepository;
    private final MarketQuoteStore quoteStore;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();
    private final MarketSnapshotProjector snapshotProjector =
        new MarketSnapshotProjector(tradePlanner);
    private final MarketPlayerResolver playerResolver;
    private final MarketCatalogInitializer catalogInitializer;
    private final boolean marketEnabled;
    private final long quoteTtlSeconds;

    public MarketService(
        MarketItemRepository marketItemRepository,
        BalanceRepository balanceRepository,
        MarketQuoteStore quoteStore,
        MarketQuoteRepository marketQuoteRepository,
        DefaultMarketCatalog defaultMarketCatalog,
        @Value("${craftalism.market.enabled:true}") boolean marketEnabled,
        @Value(
            "${craftalism.market.quote-ttl-seconds:60}"
        ) long quoteTtlSeconds,
        @Value(
            "${craftalism.market.trusted-minecraft-server-client-id:minecraft-server}"
        ) String trustedMinecraftServerClientId
    ) {
        this.marketItemRepository = marketItemRepository;
        this.balanceRepository = balanceRepository;
        this.quoteStore = quoteStore;
        this.marketQuoteRepository = marketQuoteRepository;
        this.marketEnabled = marketEnabled;
        this.quoteTtlSeconds = quoteTtlSeconds;
        this.playerResolver = new MarketPlayerResolver(
            trustedMinecraftServerClientId
        );
        this.catalogInitializer = new MarketCatalogInitializer(
            marketItemRepository,
            defaultMarketCatalog,
            tradePlanner
        );
    }

    @Transactional
    public void initializeCatalogIfEmpty() {
        catalogInitializer.initializeCatalogIfEmpty();
    }

    @Transactional
    public MarketSnapshotResponseDTO getSnapshot() {
        long totalStartNanos = System.nanoTime();

        MarketReadState readState = regeneratedItems();

        long projectionStartNanos = System.nanoTime();
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections =
            snapshotProjector.projections(readState.items());
        long projectionBuildNanos = System.nanoTime() - projectionStartNanos;

        long hashStartNanos = System.nanoTime();
        String snapshotVersion = snapshotProjector.snapshotVersion(
            projections
        );
        long hashNanos = System.nanoTime() - hashStartNanos;

        long totalNanos = System.nanoTime() - totalStartNanos;
        logSnapshotTiming(
            readState,
            projections,
            projectionBuildNanos,
            hashNanos,
            totalNanos
        );

        return snapshotProjector.response(projections, snapshotVersion);
    }

    @Transactional
    public MarketQuoteResponseDTO quote(
        JwtAuthenticationToken authentication,
        MarketQuoteRequestDTO request,
        String playerUuidHeader
    ) {
        ensureMarketOpen();

        UUID playerUuid = playerResolver.resolvePlayerUuid(
            authentication,
            request.playerUuid(),
            playerUuidHeader,
            this::currentSnapshotVersion
        );
        List<MarketItem> items = regeneratedItems().items();
        String currentSnapshotVersion = snapshotProjector.snapshotVersion(
            snapshotProjector.projections(items)
        );
        if (!currentSnapshotVersion.equals(request.snapshotVersion())) {
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Snapshot is no longer current.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion
            );
        }

        MarketItem item = items
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
        if (request.quantity() <= 0L) {
            throw rejection(
                MarketRejectionCode.INVALID_QUANTITY,
                "Quantity must be positive.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                currentSnapshotVersion
            );
        }

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

    @Transactional
    public MarketExecuteSuccessResponseDTO execute(
        JwtAuthenticationToken authentication,
        MarketExecuteRequestDTO request,
        String playerUuidHeader
    ) {
        ensureMarketOpen();

        UUID playerUuid = playerResolver.resolvePlayerUuid(
            authentication,
            request.playerUuid(),
            playerUuidHeader,
            this::currentSnapshotVersion
        );
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
        AppliedTrade appliedTrade = applyTrade(
            playerUuid,
            item,
            storedQuote,
            currentSnapshotVersion
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
            snapshotProjector.toSnapshotItem(item)
        );
    }

    private AppliedTrade applyTrade(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote,
        String snapshotVersion
    ) {
        tradePlanner.recomputeDerivedProjections(item);
        if (quote.side() == MarketSide.BUY) {
            MarketTradePlanner.TradePlan plan = requireFullBuyPlan(
                item,
                quote.quantity(),
                snapshotVersion
            );
            Balance balance = balanceRepository
                .findForUpdate(playerUuid)
                .orElseThrow(() ->
                    rejection(
                        MarketRejectionCode.INSUFFICIENT_FUNDS,
                        "Player does not have enough funds.",
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        currentSnapshotVersion()
                    )
                );
            verifyQuotedExecution(
                plan,
                quote,
                "Quoted buy execution no longer matches the authoritative segment traversal."
            );
            if (balance.getAmount() < plan.totalPrice()) {
                throw rejection(
                    MarketRejectionCode.INSUFFICIENT_FUNDS,
                    "Player does not have enough funds.",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    currentSnapshotVersion()
                );
            }
            balance.setAmount(balance.getAmount() - plan.totalPrice());
            balanceRepository.save(balance);
            tradePlanner.applyConsumption(plan);
            item.setVariationPercent(
                item.getVariationPercent().add(BigDecimal.valueOf(0.6))
            );
            item.setLastUpdatedAt(Instant.now());
            tradePlanner.recomputeDerivedProjections(item);
            marketItemRepository.save(item);
            return new AppliedTrade(
                plan.executedQuantity(),
                plan.unitPrice(),
                plan.totalPrice()
            );
        }

        MarketTradePlanner.TradePlan plan = requireFullSellPlan(
            item,
            quote.quantity(),
            snapshotVersion
        );
        Balance balance = balanceRepository
            .findForUpdate(playerUuid)
            .orElseGet(() -> new Balance(playerUuid, 0L));
        verifyQuotedExecution(
            plan,
            quote,
            "Quoted sell execution no longer matches the authoritative segment traversal."
        );
        balance.setUuid(playerUuid);
        balance.setAmount(balance.getAmount() + plan.totalPrice());
        balanceRepository.save(balance);
        tradePlanner.applyRestoration(plan);
        item.setVariationPercent(
            item.getVariationPercent().subtract(BigDecimal.valueOf(0.6))
        );
        item.setLastUpdatedAt(Instant.now());
        tradePlanner.recomputeDerivedProjections(item);
        marketItemRepository.save(item);
        return new AppliedTrade(
            plan.executedQuantity(),
            plan.unitPrice(),
            plan.totalPrice()
        );
    }

    private void verifyQuotedExecution(
        MarketTradePlanner.TradePlan plan,
        MarketQuoteStore.StoredQuote quote,
        String message
    ) {
        if (
            plan.totalPrice() != quote.totalPrice() ||
            plan.unitPrice() != quote.unitPrice()
        ) {
            throw new IllegalStateException(message);
        }
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
                "Requested quantity exceeds available stock.",
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
                "Requested quantity exceeds restorable capacity.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                snapshotVersion
            );
        }
        return plan;
    }

    private MarketReadState regeneratedItems() {
        long fetchStartNanos = System.nanoTime();
        List<MarketItem> items = marketItemRepository.findAllForMarketRead();
        long fetchNanos = System.nanoTime() - fetchStartNanos;

        long regenerationStartNanos = System.nanoTime();
        Instant now = Instant.now();
        int regeneratedItemCount = 0;
        for (MarketItem item : items) {
            if (regenerateItem(item, now)) {
                regeneratedItemCount++;
                marketItemRepository.save(item);
            }
        }
        long regenerationNanos = System.nanoTime() - regenerationStartNanos;
        return new MarketReadState(
            List.copyOf(items),
            fetchNanos,
            regenerationNanos,
            regeneratedItemCount
        );
    }

    private boolean regenerateItem(MarketItem item, Instant now) {
        tradePlanner.recomputeDerivedProjections(item);
        if (
            item.getMarketMomentum() == -1L ||
            !now.isAfter(item.getLastUpdatedAt())
        ) {
            return false;
        }

        long ticks =
            Duration.between(item.getLastUpdatedAt(), now).getSeconds() /
            STOCK_REGEN_SPEED_SECONDS;
        if (ticks <= 0L) {
            return false;
        }

        long regenQuantity = Math.multiplyExact(
            ticks,
            Math.addExact(
                BASE_STOCK_REGEN_QUANTITY,
                Math.max(item.getMarketMomentum(), 0L)
            )
        );
        MarketTradePlanner.TradePlan plan = tradePlanner.sellPlan(
            item,
            regenQuantity
        );
        if (plan.executedQuantity() <= 0L) {
            return false;
        }

        tradePlanner.applyRestoration(plan);
        item.setLastUpdatedAt(now);
        tradePlanner.recomputeDerivedProjections(item);
        return true;
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

    private String currentSnapshotVersion() {
        return snapshotProjector.snapshotVersion(
            snapshotProjector.projections(regeneratedItems().items())
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

    private void logSnapshotTiming(
        MarketReadState readState,
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections,
        long projectionBuildNanos,
        long hashNanos,
        long totalNanos
    ) {
        long segmentCount = 0L;
        for (
            MarketSnapshotProjector.MarketSnapshotProjection projection : projections
        ) {
            segmentCount += projection.segments().size();
        }

        log.info(
            "market.snapshot.timing totalMs={} fetchMs={} regenerationMs={} projectionBuildMs={} hashMs={} items={} segments={} regeneratedItems={}",
            nanosToMillis(totalNanos),
            nanosToMillis(readState.fetchNanos()),
            nanosToMillis(readState.regenerationNanos()),
            nanosToMillis(projectionBuildNanos),
            nanosToMillis(hashNanos),
            projections.size(),
            segmentCount,
            readState.regeneratedItemCount()
        );
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
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

    private record AppliedTrade(
        long executedQuantity,
        long unitPrice,
        long totalPrice
    ) {}

    private record MarketReadState(
        List<MarketItem> items,
        long fetchNanos,
        long regenerationNanos,
        int regeneratedItemCount
    ) {}
}
