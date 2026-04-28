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
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final String WRITE_SCOPE_AUTHORITY = "SCOPE_api:write";
    private static final long LEGACY_SEGMENT_CAPACITY = 50L;
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
        if (marketItemRepository.count() == 0) {
            marketItemRepository.save(seedItem("wheat", "farming", "Farming", "Wheat", "WHEAT", "2.3", 5L, 37));
            marketItemRepository.save(seedItem("carrot", "farming", "Farming", "Carrot", "CARROT", "-1.4", 1L, 29));
            marketItemRepository.save(seedItem("iron_ingot", "mining", "Mining", "Iron Ingot", "IRON_INGOT", "1.1", 14L, 13));
            return;
        }

        backfillMissingSegments();
    }

    @Transactional
    public MarketSnapshotResponseDTO getSnapshot() {
        long totalStartNanos = System.nanoTime();

        MarketReadState readState = regeneratedItems();

        long projectionStartNanos = System.nanoTime();
        List<MarketSnapshotProjection> projections = snapshotProjections(readState.items());
        Instant generatedAt = projections
            .stream()
            .map(MarketSnapshotProjection::lastUpdatedAt)
            .max(Instant::compareTo)
            .orElse(Instant.now());

        Map<String, MarketSnapshotCategoryDTO> categories = new LinkedHashMap<>();
        for (MarketSnapshotProjection item : projections) {
            MarketSnapshotCategoryDTO category = categories.computeIfAbsent(
                item.categoryId(),
                ignored ->
                    new MarketSnapshotCategoryDTO(
                        item.categoryId(),
                        item.categoryDisplayName(),
                        new ArrayList<>()
                    )
            );
            category.items().add(toSnapshotItem(item));
        }
        long projectionBuildNanos = System.nanoTime() - projectionStartNanos;

        long hashStartNanos = System.nanoTime();
        String snapshotVersion = snapshotVersion(projections);
        long hashNanos = System.nanoTime() - hashStartNanos;

        long totalNanos = System.nanoTime() - totalStartNanos;
        logSnapshotTiming(readState, projections, projectionBuildNanos, hashNanos, totalNanos);

        return new MarketSnapshotResponseDTO(snapshotVersion, generatedAt, List.copyOf(categories.values()));
    }

    @Transactional
    public MarketQuoteResponseDTO quote(
        JwtAuthenticationToken authentication,
        MarketQuoteRequestDTO request,
        String playerUuidHeader
    ) {
        ensureMarketOpen();

        UUID playerUuid = resolvePlayerUuid(authentication, request.playerUuid(), playerUuidHeader);
        List<MarketItem> items = regeneratedItems().items();
        String currentSnapshotVersion = snapshotVersion(snapshotProjections(items));
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

        TradePlan plan = request.side() == MarketSide.BUY
            ? requireFullBuyPlan(item, request.quantity(), currentSnapshotVersion)
            : requireFullSellPlan(item, request.quantity(), currentSnapshotVersion);

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
        AppliedTrade appliedTrade = applyTrade(playerUuid, item, storedQuote, currentSnapshotVersion);

        return new MarketExecuteSuccessResponseDTO(
            "SUCCESS",
            item.getItemId(),
            storedQuote.side(),
            appliedTrade.executedQuantity(),
            Long.toString(appliedTrade.unitPrice()),
            Long.toString(appliedTrade.totalPrice()),
            item.getCurrency(),
            currentSnapshotVersion(),
            toSnapshotItem(item)
        );
    }

    private AppliedTrade applyTrade(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote,
        String snapshotVersion
    ) {
        recomputeDerivedProjections(item);
        if (quote.side() == MarketSide.BUY) {
            TradePlan plan = requireFullBuyPlan(item, quote.quantity(), snapshotVersion);
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
            verifyQuotedExecution(plan, quote, "Quoted buy execution no longer matches the authoritative segment traversal.");
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
            applyConsumption(plan);
            item.setVariationPercent(item.getVariationPercent().add(BigDecimal.valueOf(0.6)));
            item.setLastUpdatedAt(Instant.now());
            recomputeDerivedProjections(item);
            marketItemRepository.save(item);
            return new AppliedTrade(plan.executedQuantity(), plan.unitPrice(), plan.totalPrice());
        }

        TradePlan plan = requireFullSellPlan(item, quote.quantity(), snapshotVersion);
        Balance balance = balanceRepository
            .findForUpdate(playerUuid)
            .orElseGet(() -> new Balance(playerUuid, 0L));
        verifyQuotedExecution(plan, quote, "Quoted sell execution no longer matches the authoritative segment traversal.");
        balance.setUuid(playerUuid);
        balance.setAmount(balance.getAmount() + plan.totalPrice());
        balanceRepository.save(balance);
        applyRestoration(plan);
        item.setVariationPercent(item.getVariationPercent().subtract(BigDecimal.valueOf(0.6)));
        item.setLastUpdatedAt(Instant.now());
        recomputeDerivedProjections(item);
        marketItemRepository.save(item);
        return new AppliedTrade(plan.executedQuantity(), plan.unitPrice(), plan.totalPrice());
    }

    private void verifyQuotedExecution(TradePlan plan, MarketQuoteStore.StoredQuote quote, String message) {
        if (plan.totalPrice() != quote.totalPrice() || plan.unitPrice() != quote.unitPrice()) {
            throw invariantViolation(message);
        }
    }

    private TradePlan requireFullBuyPlan(MarketItem item, long requestedQuantity, String snapshotVersion) {
        TradePlan plan = buyPlan(item, requestedQuantity);
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

    private TradePlan requireFullSellPlan(MarketItem item, long requestedQuantity, String snapshotVersion) {
        TradePlan plan = sellPlan(item, requestedQuantity);
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

    private TradePlan buyPlan(MarketItem item, long requestedQuantity) {
        recomputeDerivedProjections(item);
        long remainingRequest = requestedQuantity;
        long totalPrice = 0L;
        long executedQuantity = 0L;
        long totalAvailableQuantity = item.getCurrentStock();
        List<SegmentDelta> deltas = new ArrayList<>();

        for (MarketSegment segment : sortedSegments(item)) {
            if (remainingRequest <= 0L) {
                break;
            }
            if (segment.getRemainingCapacity() <= 0L) {
                continue;
            }
            long take = Math.min(remainingRequest, segment.getRemainingCapacity());
            totalPrice = Math.addExact(totalPrice, Math.multiplyExact(take, segment.getUnitPrice()));
            executedQuantity = Math.addExact(executedQuantity, take);
            remainingRequest -= take;
            deltas.add(new SegmentDelta(segment, take));
        }

        long unitPrice = executedQuantity == 0L ? 0L : effectiveUnitPrice(totalPrice, executedQuantity);
        return new TradePlan(executedQuantity, unitPrice, totalPrice, totalAvailableQuantity, deltas);
    }

    private TradePlan sellPlan(MarketItem item, long requestedQuantity) {
        recomputeDerivedProjections(item);
        long remainingRequest = requestedQuantity;
        long totalPrice = 0L;
        long executedQuantity = 0L;
        long totalAvailableQuantity = totalRestorableCapacity(item);
        List<SegmentDelta> deltas = new ArrayList<>();

        List<MarketSegment> segments = sortedSegments(item);
        for (int index = segments.size() - 1; index >= 0 && remainingRequest > 0L; index--) {
            MarketSegment segment = segments.get(index);
            long restorable = segment.getMaxCapacity() - segment.getRemainingCapacity();
            if (restorable <= 0L) {
                continue;
            }
            long take = Math.min(remainingRequest, restorable);
            totalPrice = Math.addExact(totalPrice, Math.multiplyExact(take, segment.getUnitPrice()));
            executedQuantity = Math.addExact(executedQuantity, take);
            remainingRequest -= take;
            deltas.add(new SegmentDelta(segment, take));
        }

        long unitPrice = executedQuantity == 0L ? 0L : effectiveUnitPrice(totalPrice, executedQuantity);
        return new TradePlan(executedQuantity, unitPrice, totalPrice, totalAvailableQuantity, deltas);
    }

    private void applyConsumption(TradePlan plan) {
        for (SegmentDelta delta : plan.deltas()) {
            delta.segment().setRemainingCapacity(delta.segment().getRemainingCapacity() - delta.quantity());
        }
    }

    private void applyRestoration(TradePlan plan) {
        for (SegmentDelta delta : plan.deltas()) {
            delta.segment().setRemainingCapacity(delta.segment().getRemainingCapacity() + delta.quantity());
        }
    }

    private long effectiveUnitPrice(long totalPrice, long quantity) {
        return Math.floorDiv(Math.addExact(totalPrice, quantity - 1L), quantity);
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
        return new MarketReadState(List.copyOf(items), fetchNanos, regenerationNanos, regeneratedItemCount);
    }

    private boolean regenerateItem(MarketItem item, Instant now) {
        recomputeDerivedProjections(item);
        if (item.getMarketMomentum() == -1L || !now.isAfter(item.getLastUpdatedAt())) {
            return false;
        }

        long ticks = Duration.between(item.getLastUpdatedAt(), now).getSeconds() / STOCK_REGEN_SPEED_SECONDS;
        if (ticks <= 0L) {
            return false;
        }

        long regenQuantity = Math.multiplyExact(
            ticks,
            Math.addExact(BASE_STOCK_REGEN_QUANTITY, Math.max(item.getMarketMomentum(), 0L))
        );
        TradePlan plan = sellPlan(item, regenQuantity);
        if (plan.executedQuantity() <= 0L) {
            return false;
        }

        applyRestoration(plan);
        item.setLastUpdatedAt(now);
        recomputeDerivedProjections(item);
        return true;
    }

    private void validateItemAvailability(MarketItem item, String snapshotVersion) {
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

    private void backfillMissingSegments() {
        List<MarketItem> items = marketItemRepository.findAllForMarketRead();
        boolean changed = false;
        for (MarketItem item : items) {
            if (!item.getSegments().isEmpty()) {
                recomputeDerivedProjections(item);
                continue;
            }
            item.setSegments(legacyBackfillSegments(item));
            recomputeDerivedProjections(item);
            changed = true;
        }
        if (changed) {
            marketItemRepository.saveAll(items);
        }
    }

    private List<MarketSegment> legacyBackfillSegments(MarketItem item) {
        long consumedQuantity = item.getMarketMomentum() < 0L ? 0L : item.getMarketMomentum();
        long totalCapacity = Math.addExact(item.getCurrentStock(), consumedQuantity);
        long segmentCount = Math.max(1L, divideRoundUp(Math.max(totalCapacity, 1L), LEGACY_SEGMENT_CAPACITY));
        long legacyBasePrice = Math.max(
            1L,
            item.getBuyUnitEstimate() - Math.floorDiv(consumedQuantity, LEGACY_SEGMENT_CAPACITY)
        );
        long remainingConsumed = consumedQuantity;
        long remainingCapacityBudget = totalCapacity;
        List<MarketSegment> segments = new ArrayList<>();

        for (long segmentIndex = 0L; segmentIndex < segmentCount; segmentIndex++) {
            long capacity = Math.min(LEGACY_SEGMENT_CAPACITY, Math.max(remainingCapacityBudget, 1L));
            long consumedInSegment = Math.min(capacity, remainingConsumed);
            MarketSegment segment = new MarketSegment();
            segment.setSegmentIndex(segmentIndex);
            segment.setMaxCapacity(capacity);
            segment.setRemainingCapacity(capacity - consumedInSegment);
            segment.setUnitPrice(Math.addExact(legacyBasePrice, segmentIndex));
            segments.add(segment);
            remainingConsumed -= consumedInSegment;
            remainingCapacityBudget = Math.max(0L, remainingCapacityBudget - capacity);
        }

        if (remainingConsumed != 0L) {
            throw invariantViolation("Legacy market state could not be deterministically backfilled into segments.");
        }

        return segments;
    }

    private long divideRoundUp(long numerator, long denominator) {
        return Math.floorDiv(Math.addExact(numerator, denominator - 1L), denominator);
    }

    private void recomputeDerivedProjections(MarketItem item) {
        List<MarketSegment> segments = sortedSegments(item);
        if (segments.isEmpty()) {
            throw invariantViolation("Market item must have at least one segment.");
        }

        long expectedIndex = 0L;
        long currentStock = 0L;
        int partialSegments = 0;
        SegmentState phase = SegmentState.CONSUMED;
        long buyFrontier = -1L;
        long restoreFrontier = -1L;

        for (MarketSegment segment : segments) {
            if (segment.getSegmentIndex() != expectedIndex) {
                throw invariantViolation("Segment indexes must be contiguous and start at zero.");
            }
            if (segment.getMaxCapacity() <= 0L) {
                throw invariantViolation("Segment max capacity must be positive.");
            }
            if (segment.getUnitPrice() <= 0L) {
                throw invariantViolation("Segment unit price must be positive.");
            }
            if (segment.getRemainingCapacity() < 0L || segment.getRemainingCapacity() > segment.getMaxCapacity()) {
                throw invariantViolation("Segment remaining capacity must stay within bounds.");
            }

            currentStock = Math.addExact(currentStock, segment.getRemainingCapacity());
            if (segment.getRemainingCapacity() > 0L && buyFrontier == -1L) {
                buyFrontier = segment.getSegmentIndex();
            }
            if (segment.getRemainingCapacity() < segment.getMaxCapacity()) {
                restoreFrontier = segment.getSegmentIndex();
            }

            SegmentState state = stateOf(segment);
            if (state == SegmentState.PARTIAL) {
                partialSegments++;
                if (partialSegments > 1 || phase == SegmentState.UNTOUCHED) {
                    throw invariantViolation("There must be at most one partially consumed segment and no gaps.");
                }
                phase = SegmentState.PARTIAL;
            } else if (state == SegmentState.UNTOUCHED) {
                phase = SegmentState.UNTOUCHED;
            } else if (phase != SegmentState.CONSUMED) {
                throw invariantViolation("Consumed segments cannot appear after partial or untouched segments.");
            }

            expectedIndex++;
        }

        item.setCurrentStock(currentStock);
        item.setMarketMomentum(restoreFrontier);
        item.setBuyUnitEstimate(frontierUnitPrice(segments, buyFrontier, segments.get(segments.size() - 1).getUnitPrice()));
        item.setSellUnitEstimate(frontierUnitPrice(segments, restoreFrontier, segments.get(0).getUnitPrice()));
    }

    private long frontierUnitPrice(List<MarketSegment> segments, long frontier, long fallback) {
        if (frontier < 0L) {
            return fallback;
        }
        return segments.get(Math.toIntExact(frontier)).getUnitPrice();
    }

    private SegmentState stateOf(MarketSegment segment) {
        if (segment.getRemainingCapacity() == 0L) {
            return SegmentState.CONSUMED;
        }
        if (segment.getRemainingCapacity() == segment.getMaxCapacity()) {
            return SegmentState.UNTOUCHED;
        }
        return SegmentState.PARTIAL;
    }

    private long totalRestorableCapacity(MarketItem item) {
        long total = 0L;
        for (MarketSegment segment : item.getSegments()) {
            total = Math.addExact(total, segment.getMaxCapacity() - segment.getRemainingCapacity());
        }
        return total;
    }

    private List<MarketSegment> sortedSegments(MarketItem item) {
        return item.getSegments()
            .stream()
            .sorted(Comparator.comparingLong(MarketSegment::getSegmentIndex))
            .toList();
    }

    private IllegalStateException invariantViolation(String message) {
        return new IllegalStateException(message);
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
        recomputeDerivedProjections(item);
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

    private MarketSnapshotItemDTO toSnapshotItem(MarketSnapshotProjection item) {
        return new MarketSnapshotItemDTO(
            item.itemId(),
            item.displayName(),
            item.iconKey(),
            Long.toString(item.buyUnitEstimate()),
            Long.toString(item.sellUnitEstimate()),
            item.currency(),
            item.currentStock(),
            item.variationPercent(),
            item.blocked(),
            item.operating(),
            item.lastUpdatedAt()
        );
    }

    private String currentSnapshotVersion() {
        return snapshotVersion(snapshotProjections(regeneratedItems().items()));
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

    private List<MarketSnapshotProjection> snapshotProjections(List<MarketItem> items) {
        List<MarketSnapshotProjection> projections = new ArrayList<>(items.size());
        for (MarketItem item : items) {
            recomputeDerivedProjections(item);
            projections.add(
                new MarketSnapshotProjection(
                    item.getItemId(),
                    item.getCategoryId(),
                    item.getCategoryDisplayName(),
                    item.getDisplayName(),
                    item.getIconKey(),
                    item.getBuyUnitEstimate(),
                    item.getSellUnitEstimate(),
                    item.getCurrency(),
                    item.getCurrentStock(),
                    item.getMarketMomentum(),
                    item.getVariationPercent().stripTrailingZeros().toPlainString(),
                    item.isBlocked(),
                    item.isOperating(),
                    item.getLastUpdatedAt(),
                    sortedSegments(item)
                        .stream()
                        .map(segment ->
                            new MarketSegmentProjection(
                                segment.getSegmentIndex(),
                                segment.getMaxCapacity(),
                                segment.getRemainingCapacity(),
                                segment.getUnitPrice()
                            )
                        )
                        .toList()
                )
            );
        }
        return List.copyOf(projections);
    }

    private String snapshotVersion(List<MarketSnapshotProjection> items) {
        StringBuilder payload = new StringBuilder("market");
        for (MarketSnapshotProjection item : items) {
            payload
                .append('|')
                .append(item.itemId())
                .append(':')
                .append(item.currentStock())
                .append(':')
                .append(item.buyUnitEstimate())
                .append(':')
                .append(item.sellUnitEstimate())
                .append(':')
                .append(item.marketMomentum())
                .append(':')
                .append(item.blocked())
                .append(':')
                .append(item.operating())
                .append(':')
                .append(item.lastUpdatedAt());
            for (MarketSegmentProjection segment : item.segments()) {
                payload
                    .append(':')
                    .append(segment.segmentIndex())
                    .append(',')
                    .append(segment.maxCapacity())
                    .append(',')
                    .append(segment.remainingCapacity())
                    .append(',')
                    .append(segment.unitPrice());
            }
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            return "market:" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private void logSnapshotTiming(
        MarketReadState readState,
        List<MarketSnapshotProjection> projections,
        long projectionBuildNanos,
        long hashNanos,
        long totalNanos
    ) {
        long segmentCount = 0L;
        for (MarketSnapshotProjection projection : projections) {
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

    private MarketItem seedItem(
        String itemId,
        String categoryId,
        String categoryDisplayName,
        String displayName,
        String iconKey,
        String variationPercent,
        long baseUnitPrice,
        int segmentCount
    ) {
        MarketItem item = new MarketItem();
        item.setItemId(itemId);
        item.setCategoryId(categoryId);
        item.setCategoryDisplayName(categoryDisplayName);
        item.setDisplayName(displayName);
        item.setIconKey(iconKey);
        item.setCurrency("coins");
        item.setVariationPercent(new BigDecimal(variationPercent));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.now());
        item.setSegments(explicitSeedSegments(baseUnitPrice, segmentCount));
        recomputeDerivedProjections(item);
        return item;
    }

    private List<MarketSegment> explicitSeedSegments(long baseUnitPrice, int segmentCount) {
        List<MarketSegment> segments = new ArrayList<>();
        for (int index = 0; index < segmentCount; index++) {
            MarketSegment segment = new MarketSegment();
            segment.setSegmentIndex(index);
            segment.setMaxCapacity(LEGACY_SEGMENT_CAPACITY);
            segment.setRemainingCapacity(LEGACY_SEGMENT_CAPACITY);
            segment.setUnitPrice(baseUnitPrice + index);
            segments.add(segment);
        }
        return segments;
    }

    private MarketRejectionException rejection(
        MarketRejectionCode code,
        String message,
        HttpStatus status,
        String snapshotVersion
    ) {
        return new MarketRejectionException(code, message, status, snapshotVersion);
    }

    private enum SegmentState {
        CONSUMED,
        PARTIAL,
        UNTOUCHED
    }

    private record SegmentDelta(MarketSegment segment, long quantity) {}

    private record TradePlan(
        long executedQuantity,
        long unitPrice,
        long totalPrice,
        long totalAvailableQuantity,
        List<SegmentDelta> deltas
    ) {}

    private record AppliedTrade(long executedQuantity, long unitPrice, long totalPrice) {}

    private record MarketSegmentProjection(long segmentIndex, long maxCapacity, long remainingCapacity, long unitPrice) {}

    private record MarketSnapshotProjection(
        String itemId,
        String categoryId,
        String categoryDisplayName,
        String displayName,
        String iconKey,
        long buyUnitEstimate,
        long sellUnitEstimate,
        String currency,
        long currentStock,
        long marketMomentum,
        String variationPercent,
        boolean blocked,
        boolean operating,
        Instant lastUpdatedAt,
        List<MarketSegmentProjection> segments
    ) {}

    private record MarketReadState(
        List<MarketItem> items,
        long fetchNanos,
        long regenerationNanos,
        int regeneratedItemCount
    ) {}
}
