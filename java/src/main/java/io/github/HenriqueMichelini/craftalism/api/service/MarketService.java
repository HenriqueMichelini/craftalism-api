package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotCategoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotItemDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketService {

    private static final long QUOTE_TTL_SECONDS = 60L;
    private static final String WRITE_SCOPE_AUTHORITY = "SCOPE_api:write";
    private static final long SEGMENT_CAPACITY = 50L;
    private static final long STOCK_REGEN_SPEED_SECONDS = 60L;
    private static final long BASE_STOCK_REGEN_QUANTITY = 1L;

    private final MarketItemRepository marketItemRepository;
    private final BalanceRepository balanceRepository;
    private final MarketQuoteStore quoteStore;
    private final MarketQuoteRepository marketQuoteRepository;
    private final boolean marketEnabled;
    private final long quoteTtlSeconds;
    private final String trustedMinecraftServerClientId;

    public MarketService(
        MarketItemRepository marketItemRepository,
        BalanceRepository balanceRepository,
        MarketQuoteStore quoteStore,
        MarketQuoteRepository marketQuoteRepository,
        @Value("${craftalism.market.enabled:true}") boolean marketEnabled,
        @Value("${craftalism.market.quote-ttl-seconds:60}") long quoteTtlSeconds,
        @Value("${craftalism.market.trusted-minecraft-server-client-id:minecraft-server}") String trustedMinecraftServerClientId
    ) {
        this.marketItemRepository = marketItemRepository;
        this.balanceRepository = balanceRepository;
        this.quoteStore = quoteStore;
        this.marketQuoteRepository = marketQuoteRepository;
        this.marketEnabled = marketEnabled;
        this.quoteTtlSeconds = quoteTtlSeconds;
        this.trustedMinecraftServerClientId = trustedMinecraftServerClientId;
    }

    @Transactional
    public void initializeCatalogIfEmpty() {
        if (marketItemRepository.count() > 0) {
            return;
        }

        marketItemRepository.save(seedItem("wheat", "farming", "Farming", "Wheat", "WHEAT", 5, 4, 1820, "2.3"));
        marketItemRepository.save(seedItem("carrot", "farming", "Farming", "Carrot", "CARROT", 6, 4, 1410, "-1.4"));
        marketItemRepository.save(seedItem("iron_ingot", "mining", "Mining", "Iron Ingot", "IRON_INGOT", 14, 11, 620, "1.1"));
    }

    @Transactional
    public MarketSnapshotResponseDTO getSnapshot() {
        initializeCatalogIfEmpty();

        List<MarketItem> items = regeneratedItems();
        String snapshotVersion = snapshotVersion(items);
        Instant generatedAt = items
            .stream()
            .map(MarketItem::getLastUpdatedAt)
            .max(Instant::compareTo)
            .orElse(Instant.now());

        Map<String, MarketSnapshotCategoryDTO> categories = new LinkedHashMap<>();
        for (MarketItem item : items) {
            MarketSnapshotCategoryDTO category = categories.computeIfAbsent(
                item.getCategoryId(),
                ignored ->
                    new MarketSnapshotCategoryDTO(
                        item.getCategoryId(),
                        item.getCategoryDisplayName(),
                        new java.util.ArrayList<>()
                    )
            );
            category.items().add(toSnapshotItem(item));
        }

        return new MarketSnapshotResponseDTO(
            snapshotVersion,
            generatedAt,
            List.copyOf(categories.values())
        );
    }

    @Transactional
    public MarketQuoteResponseDTO quote(
        JwtAuthenticationToken authentication,
        MarketQuoteRequestDTO request,
        String playerUuidHeader
    ) {
        initializeCatalogIfEmpty();
        ensureMarketOpen();

        UUID playerUuid = resolvePlayerUuid(authentication, request.playerUuid(), playerUuidHeader);
        List<MarketItem> items = regeneratedItems();
        String currentSnapshotVersion = snapshotVersion(items);
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

        long quantity = request.quantity();
        if (quantity <= 0) {
            throw rejection(
                MarketRejectionCode.INVALID_QUANTITY,
                "Quantity must be positive.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                currentSnapshotVersion
            );
        }
        MarketPriceQuote priceQuote = quotePrice(item, request.side(), quantity);
        Instant expiresAt = Instant.now().plusSeconds(quoteTtlSeconds);
        String quoteToken = UUID.randomUUID().toString();

        quoteStore.put(
            new MarketQuoteStore.StoredQuote(
                quoteToken,
                playerUuid,
                item.getItemId(),
                request.side(),
                quantity,
                priceQuote.unitPrice(),
                priceQuote.totalPrice(),
                currentSnapshotVersion,
                expiresAt,
                MarketQuote.Status.ACTIVE
            )
        );

        return new MarketQuoteResponseDTO(
            item.getItemId(),
            request.side(),
            quantity,
            Long.toString(priceQuote.unitPrice()),
            Long.toString(priceQuote.totalPrice()),
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
        initializeCatalogIfEmpty();
        ensureMarketOpen();

        UUID playerUuid = resolvePlayerUuid(authentication, request.playerUuid(), playerUuidHeader);
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
        applyTrade(playerUuid, item, storedQuote);

        MarketSnapshotItemDTO updatedItem = toSnapshotItem(item);
        return new MarketExecuteSuccessResponseDTO(
            "SUCCESS",
            item.getItemId(),
            storedQuote.side(),
            storedQuote.quantity(),
            Long.toString(storedQuote.unitPrice()),
            Long.toString(storedQuote.totalPrice()),
            item.getCurrency(),
            currentSnapshotVersion(),
            updatedItem
        );
    }

    private void applyTrade(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote
    ) {
        if (quote.side() == MarketSide.BUY) {
            long baseBuyPrice = baseBuyPrice(item);
            long baseSellPrice = baseSellPrice(item);
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
            if (balance.getAmount() < quote.totalPrice()) {
                throw rejection(
                    MarketRejectionCode.INSUFFICIENT_FUNDS,
                    "Player does not have enough funds.",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    currentSnapshotVersion()
                );
            }
            balance.setAmount(balance.getAmount() - quote.totalPrice());
            balanceRepository.save(balance);
            item.setCurrentStock(item.getCurrentStock() - quote.quantity());
            item.setMarketMomentum(Math.addExact(item.getMarketMomentum(), quote.quantity()));
            item.setBuyUnitEstimate(buyUnitPrice(baseBuyPrice, currentSegment(item.getMarketMomentum())));
            item.setSellUnitEstimate(sellUnitPrice(baseSellPrice, currentSegment(item.getMarketMomentum())));
            item.setVariationPercent(item.getVariationPercent().add(java.math.BigDecimal.valueOf(0.6)));
        } else {
            long baseBuyPrice = baseBuyPrice(item);
            long baseSellPrice = baseSellPrice(item);
            Balance balance = balanceRepository
                .findForUpdate(playerUuid)
                .orElseGet(() -> new Balance(playerUuid, 0L));
            balance.setUuid(playerUuid);
            balance.setAmount(balance.getAmount() + quote.totalPrice());
            balanceRepository.save(balance);
            item.setCurrentStock(item.getCurrentStock() + quote.quantity());
            item.setMarketMomentum(Math.max(0L, item.getMarketMomentum() - quote.quantity()));
            item.setBuyUnitEstimate(buyUnitPrice(baseBuyPrice, currentSegment(item.getMarketMomentum())));
            item.setSellUnitEstimate(sellUnitPrice(baseSellPrice, currentSegment(item.getMarketMomentum())));
            item.setVariationPercent(item.getVariationPercent().subtract(java.math.BigDecimal.valueOf(0.6)));
        }

        item.setLastUpdatedAt(Instant.now());
        marketItemRepository.save(item);
    }

    private MarketPriceQuote quotePrice(MarketItem item, MarketSide side, long quantity) {
        long totalPrice = side == MarketSide.BUY
            ? progressiveBuyTotal(item, quantity)
            : progressiveSellTotal(item, quantity);
        return new MarketPriceQuote(effectiveUnitPrice(totalPrice, quantity), totalPrice);
    }

    private long progressiveBuyTotal(MarketItem item, long quantity) {
        long remaining = quantity;
        long momentumCursor = item.getMarketMomentum();
        long totalPrice = 0L;

        while (remaining > 0L) {
            MarketPriceSegment segment = buySegment(item, currentSegment(momentumCursor));
            long segmentRemaining = segment.capacity() - segmentOffset(momentumCursor);
            long take = Math.min(segmentRemaining, remaining);
            totalPrice = Math.addExact(
                totalPrice,
                Math.multiplyExact(take, segment.unitPrice())
            );
            remaining -= take;
            momentumCursor = Math.addExact(momentumCursor, take);
        }

        return totalPrice;
    }

    private long progressiveSellTotal(MarketItem item, long quantity) {
        long remaining = quantity;
        long momentumCursor = item.getMarketMomentum();
        long totalPrice = 0L;

        while (remaining > 0L) {
            MarketPriceSegment segment = sellSegment(item, currentSegment(momentumCursor));
            long segmentDepth = momentumCursor == 0L
                ? segment.capacity()
                : Math.max(1L, segmentOffset(momentumCursor));
            long take = Math.min(segmentDepth, remaining);
            totalPrice = Math.addExact(
                totalPrice,
                Math.multiplyExact(take, segment.unitPrice())
            );
            remaining -= take;
            momentumCursor = Math.max(0L, momentumCursor - take);
        }

        return totalPrice;
    }

    private long effectiveUnitPrice(long totalPrice, long quantity) {
        return Math.floorDiv(Math.addExact(totalPrice, quantity - 1L), quantity);
    }

    private long currentSegment(long momentum) {
        return Math.floorDiv(momentum, SEGMENT_CAPACITY);
    }

    private long segmentOffset(long momentum) {
        return Math.floorMod(momentum, SEGMENT_CAPACITY);
    }

    private MarketPriceSegment buySegment(MarketItem item, long segment) {
        return new MarketPriceSegment(SEGMENT_CAPACITY, buyUnitPrice(item, segment));
    }

    private MarketPriceSegment sellSegment(MarketItem item, long segment) {
        return new MarketPriceSegment(SEGMENT_CAPACITY, sellUnitPrice(item, segment));
    }

    private long buyUnitPrice(MarketItem item, long segment) {
        return buyUnitPrice(baseBuyPrice(item), segment);
    }

    private long sellUnitPrice(MarketItem item, long segment) {
        return sellUnitPrice(baseSellPrice(item), segment);
    }

    private long buyUnitPrice(long basePrice, long segment) {
        return Math.addExact(basePrice, segment);
    }

    private long sellUnitPrice(long basePrice, long segment) {
        return Math.max(1L, Math.addExact(basePrice, segment));
    }

    private long baseBuyPrice(MarketItem item) {
        return Math.max(1L, item.getBuyUnitEstimate() - currentSegment(item.getMarketMomentum()));
    }

    private long baseSellPrice(MarketItem item) {
        return Math.max(1L, item.getSellUnitEstimate() - currentSegment(item.getMarketMomentum()));
    }

    private List<MarketItem> regeneratedItems() {
        List<MarketItem> items = marketItemRepository.findAllByOrderByCategoryIdAscDisplayNameAsc();
        Instant now = Instant.now();
        for (MarketItem item : items) {
            if (regenerateItem(item, now)) {
                marketItemRepository.save(item);
            }
        }
        return items;
    }

    private boolean regenerateItem(MarketItem item, Instant now) {
        if (item.getMarketMomentum() <= 0L || !now.isAfter(item.getLastUpdatedAt())) {
            return false;
        }

        long ticks = java.time.Duration.between(item.getLastUpdatedAt(), now)
            .getSeconds() / STOCK_REGEN_SPEED_SECONDS;
        if (ticks <= 0L) {
            return false;
        }

        long regenQuantity = Math.multiplyExact(
            ticks,
            Math.addExact(BASE_STOCK_REGEN_QUANTITY, currentSegment(item.getMarketMomentum()))
        );
        long baseBuyPrice = baseBuyPrice(item);
        long baseSellPrice = baseSellPrice(item);
        long restored = applySegmentRegeneration(item, regenQuantity);
        if (restored <= 0L) {
            return false;
        }

        item.setCurrentStock(Math.addExact(item.getCurrentStock(), restored));
        item.setBuyUnitEstimate(buyUnitPrice(baseBuyPrice, currentSegment(item.getMarketMomentum())));
        item.setSellUnitEstimate(sellUnitPrice(baseSellPrice, currentSegment(item.getMarketMomentum())));
        item.setLastUpdatedAt(now);
        return true;
    }

    private long applySegmentRegeneration(MarketItem item, long regenQuantity) {
        long remaining = regenQuantity;
        long restored = 0L;
        long momentumCursor = item.getMarketMomentum();

        while (remaining > 0L && momentumCursor > 0L) {
            long segmentRestorable = segmentOffset(momentumCursor);
            if (segmentRestorable == 0L) {
                segmentRestorable = SEGMENT_CAPACITY;
            }

            long take = Math.min(segmentRestorable, remaining);
            momentumCursor -= take;
            restored += take;
            remaining -= take;
        }

        item.setMarketMomentum(momentumCursor);
        return restored;
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

    private UUID resolvePlayerUuid(
        JwtAuthenticationToken authentication,
        String suppliedPlayerUuid,
        String suppliedPlayerUuidHeader
    ) {
        if (authentication == null) {
            throw rejection(
                MarketRejectionCode.API_UNAVAILABLE,
                "Authenticated player context is unavailable.",
                HttpStatus.SERVICE_UNAVAILABLE,
                currentSnapshotVersion()
            );
        }

        Object playerUuidClaim = authentication.getTokenAttributes().get("player_uuid");
        if (playerUuidClaim instanceof String claimValue) {
            Optional<UUID> parsed = tryParseUuid(claimValue);
            if (parsed.isPresent()) {
                return parsed.get();
            }
        }

        Optional<UUID> subject = tryParseUuid(authentication.getName());
        if (subject.isPresent()) {
            return subject.get();
        }

        Optional<String> supplied = firstText(suppliedPlayerUuid, suppliedPlayerUuidHeader);
        if (supplied.isPresent() && isTrustedMinecraftServer(authentication)) {
            return tryParseUuid(supplied.get())
                .orElseThrow(() ->
                    rejection(
                        MarketRejectionCode.API_UNAVAILABLE,
                        "Authenticated player context is unavailable.",
                        HttpStatus.SERVICE_UNAVAILABLE,
                        currentSnapshotVersion()
                    )
                );
        }

        throw rejection(
            MarketRejectionCode.API_UNAVAILABLE,
            "Authenticated player context is unavailable.",
            HttpStatus.SERVICE_UNAVAILABLE,
            currentSnapshotVersion()
        );
    }

    private boolean isTrustedMinecraftServer(JwtAuthenticationToken authentication) {
        return isTrustedClientIdentity(authentication) &&
            authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority -> WRITE_SCOPE_AUTHORITY.equals(authority.getAuthority()));
    }

    private boolean isTrustedClientIdentity(JwtAuthenticationToken authentication) {
        return trustedMinecraftServerClientId.equals(authentication.getName()) ||
            trustedMinecraftServerClientId.equals(authentication.getTokenAttributes().get("client_id")) ||
            trustedMinecraftServerClientId.equals(authentication.getTokenAttributes().get("azp"));
    }

    private Optional<String> firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return Optional.of(first.trim());
        }
        if (second != null && !second.isBlank()) {
            return Optional.of(second.trim());
        }
        return Optional.empty();
    }

    private Optional<UUID> tryParseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private MarketSnapshotItemDTO toSnapshotItem(MarketItem item) {
        return new MarketSnapshotItemDTO(
            item.getItemId(),
            item.getDisplayName(),
            item.getIconKey(),
            Long.toString(item.getBuyUnitEstimate()),
            Long.toString(item.getSellUnitEstimate()),
            item.getCurrency(),
            item.getCurrentStock(),
            item.getVariationPercent().stripTrailingZeros().toPlainString(),
            item.isBlocked(),
            item.isOperating(),
            item.getLastUpdatedAt()
        );
    }

    private String currentSnapshotVersion() {
        initializeCatalogIfEmpty();
        return snapshotVersion(
            regeneratedItems()
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

    private String snapshotVersion(List<MarketItem> items) {
        StringBuilder payload = new StringBuilder("market");
        for (MarketItem item : items) {
            payload
                .append('|')
                .append(item.getItemId())
                .append(':')
                .append(item.getCurrentStock())
                .append(':')
                .append(item.getBuyUnitEstimate())
                .append(':')
                .append(item.getSellUnitEstimate())
                .append(':')
                .append(item.getMarketMomentum())
                .append(':')
                .append(item.isBlocked())
                .append(':')
                .append(item.isOperating())
                .append(':')
                .append(item.getLastUpdatedAt());
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                payload.toString().getBytes(StandardCharsets.UTF_8)
            );
            return "market:" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private MarketItem seedItem(
        String itemId,
        String categoryId,
        String categoryDisplayName,
        String displayName,
        String iconKey,
        long buyUnitEstimate,
        long sellUnitEstimate,
        long currentStock,
        String variationPercent
    ) {
        MarketItem item = new MarketItem();
        item.setItemId(itemId);
        item.setCategoryId(categoryId);
        item.setCategoryDisplayName(categoryDisplayName);
        item.setDisplayName(displayName);
        item.setIconKey(iconKey);
        item.setBuyUnitEstimate(buyUnitEstimate);
        item.setSellUnitEstimate(sellUnitEstimate);
        item.setCurrency("coins");
        item.setCurrentStock(currentStock);
        item.setMarketMomentum(0L);
        item.setVariationPercent(new java.math.BigDecimal(variationPercent));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.now());
        return item;
    }

    private MarketRejectionException rejection(
        MarketRejectionCode code,
        String message,
        HttpStatus status,
        String snapshotVersion
    ) {
        return new MarketRejectionException(code, message, status, snapshotVersion);
    }

    private record MarketPriceQuote(long unitPrice, long totalPrice) {}

    private record MarketPriceSegment(long capacity, long unitPrice) {}
}
