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
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
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

    private final MarketItemRepository marketItemRepository;
    private final BalanceRepository balanceRepository;
    private final MarketQuoteStore quoteStore;
    private final boolean marketEnabled;

    public MarketService(
        MarketItemRepository marketItemRepository,
        BalanceRepository balanceRepository,
        MarketQuoteStore quoteStore,
        @Value("${craftalism.market.enabled:true}") boolean marketEnabled
    ) {
        this.marketItemRepository = marketItemRepository;
        this.balanceRepository = balanceRepository;
        this.quoteStore = quoteStore;
        this.marketEnabled = marketEnabled;
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

        List<MarketItem> items = marketItemRepository.findAllByOrderByCategoryIdAscDisplayNameAsc();
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
        MarketQuoteRequestDTO request
    ) {
        initializeCatalogIfEmpty();
        ensureMarketOpen();

        UUID playerUuid = resolvePlayerUuid(authentication);
        List<MarketItem> items = marketItemRepository.findAllByOrderByCategoryIdAscDisplayNameAsc();
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
        if (
            request.side() == MarketSide.BUY &&
            item.getCurrentStock() < quantity
        ) {
            throw rejection(
                MarketRejectionCode.INSUFFICIENT_STOCK,
                "Item does not have enough stock for that quantity.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                currentSnapshotVersion
            );
        }

        long unitPrice = quoteUnitPrice(item, request.side(), quantity);
        long totalPrice = Math.multiplyExact(unitPrice, quantity);
        Instant expiresAt = Instant.now().plusSeconds(QUOTE_TTL_SECONDS);
        String quoteToken = UUID.randomUUID().toString();

        quoteStore.put(
            new MarketQuoteStore.StoredQuote(
                quoteToken,
                playerUuid,
                item.getItemId(),
                request.side(),
                quantity,
                unitPrice,
                totalPrice,
                currentSnapshotVersion,
                expiresAt
            )
        );

        return new MarketQuoteResponseDTO(
            item.getItemId(),
            request.side(),
            quantity,
            Long.toString(unitPrice),
            Long.toString(totalPrice),
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
        MarketExecuteRequestDTO request
    ) {
        initializeCatalogIfEmpty();
        ensureMarketOpen();

        UUID playerUuid = resolvePlayerUuid(authentication);
        MarketQuoteStore.StoredQuote storedQuote = quoteStore
            .getActive(request.quoteToken())
            .orElseThrow(() ->
                rejection(
                    MarketRejectionCode.QUOTE_EXPIRED,
                    "Quote has expired.",
                    HttpStatus.CONFLICT,
                    currentSnapshotVersion()
                )
            );

        if (
            !storedQuote.playerUuid().equals(playerUuid) ||
            !storedQuote.itemId().equals(request.itemId()) ||
            storedQuote.side() != request.side() ||
            storedQuote.quantity() != request.quantity()
        ) {
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        if (!storedQuote.snapshotVersion().equals(request.snapshotVersion())) {
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion()
            );
        }

        String currentSnapshotVersion = currentSnapshotVersion();
        if (!currentSnapshotVersion.equals(storedQuote.snapshotVersion())) {
            quoteStore.remove(request.quoteToken());
            throw rejection(
                MarketRejectionCode.STALE_QUOTE,
                "Quote is no longer valid.",
                HttpStatus.CONFLICT,
                currentSnapshotVersion
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
        quoteStore.remove(request.quoteToken());

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
            if (item.getCurrentStock() < quote.quantity()) {
                throw rejection(
                    MarketRejectionCode.INSUFFICIENT_STOCK,
                    "Item does not have enough stock for that quantity.",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    currentSnapshotVersion()
                );
            }
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
            item.setBuyUnitEstimate(item.getBuyUnitEstimate() + marketStep(quote.quantity(), 50L));
            item.setSellUnitEstimate(Math.max(1L, item.getSellUnitEstimate() + marketStep(quote.quantity(), 100L)));
            item.setVariationPercent(item.getVariationPercent().add(java.math.BigDecimal.valueOf(0.6)));
        } else {
            Balance balance = balanceRepository
                .findForUpdate(playerUuid)
                .orElseGet(() -> new Balance(playerUuid, 0L));
            balance.setUuid(playerUuid);
            balance.setAmount(balance.getAmount() + quote.totalPrice());
            balanceRepository.save(balance);
            item.setCurrentStock(item.getCurrentStock() + quote.quantity());
            item.setBuyUnitEstimate(Math.max(1L, item.getBuyUnitEstimate() - marketStep(quote.quantity(), 75L)));
            item.setSellUnitEstimate(Math.max(1L, item.getSellUnitEstimate() - marketStep(quote.quantity(), 50L)));
            item.setVariationPercent(item.getVariationPercent().subtract(java.math.BigDecimal.valueOf(0.6)));
        }

        item.setLastUpdatedAt(Instant.now());
        marketItemRepository.save(item);
    }

    private long quoteUnitPrice(MarketItem item, MarketSide side, long quantity) {
        return side == MarketSide.BUY
            ? item.getBuyUnitEstimate() + ((quantity - 1) / 50L)
            : Math.max(1L, item.getSellUnitEstimate() - ((quantity - 1) / 100L));
    }

    private long marketStep(long quantity, long divisor) {
        return Math.max(1L, quantity / divisor);
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

    private UUID resolvePlayerUuid(JwtAuthenticationToken authentication) {
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

        throw rejection(
            MarketRejectionCode.API_UNAVAILABLE,
            "Authenticated player context is unavailable.",
            HttpStatus.SERVICE_UNAVAILABLE,
            currentSnapshotVersion()
        );
    }

    private Optional<UUID> tryParseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
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
            marketItemRepository.findAllByOrderByCategoryIdAscDisplayNameAsc()
        );
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
}
