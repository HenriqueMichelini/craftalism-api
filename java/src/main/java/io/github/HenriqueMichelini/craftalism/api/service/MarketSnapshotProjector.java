package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketActiveEventContextDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotCategoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotItemDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MarketSnapshotProjector {

    private final MarketTradePlanner tradePlanner;
    private final MarketEventBlockingService eventBlockingService;

    MarketSnapshotProjector(MarketTradePlanner tradePlanner) {
        this(tradePlanner, null);
    }

    MarketSnapshotProjector(
        MarketTradePlanner tradePlanner,
        MarketEventBlockingService eventBlockingService
    ) {
        this.tradePlanner = tradePlanner;
        this.eventBlockingService = eventBlockingService;
    }

    MarketSnapshotResponseDTO response(
        List<MarketSnapshotProjection> projections,
        String snapshotVersion,
        MarketActiveEventContextDTO activeEvent
    ) {
        Instant generatedAt = projections
            .stream()
            .map(MarketSnapshotProjection::lastUpdatedAt)
            .max(Instant::compareTo)
            .orElse(Instant.now());

        Map<String, MarketSnapshotCategoryDTO> categories =
            new LinkedHashMap<>();
        for (MarketSnapshotProjection item : projections) {
            MarketSnapshotCategoryDTO category = categories.computeIfAbsent(
                item.categoryId(),
                ignored ->
                    new MarketSnapshotCategoryDTO(
                        item.categoryId(),
                        item.categoryDisplayName(),
                        item.categoryIconKey(),
                        new ArrayList<>()
                    )
            );
            category.items().add(toSnapshotItem(item));
        }

        return new MarketSnapshotResponseDTO(
            snapshotVersion,
            generatedAt,
            activeEvent,
            List.copyOf(categories.values())
        );
    }

    MarketSnapshotResponseDTO response(
        List<MarketSnapshotProjection> projections,
        String snapshotVersion
    ) {
        return response(projections, snapshotVersion, null);
    }

    MarketSnapshotItemDTO toSnapshotItem(MarketItem item) {
        clearEventCaches();
        try {
            tradePlanner.recomputeDerivedProjections(item);
            return new MarketSnapshotItemDTO(
                item.getItemId(),
                item.getDisplayName(),
                item.getIconKey(),
                Long.toString(item.getBuyUnitEstimate()),
                Long.toString(item.getSellUnitEstimate()),
                item.getCurrency(),
                item.getNetPosition(),
                tradePlanner.pressureSegment(item, item.getNetPosition()),
                pressureMagnitude(item.getNetPosition()),
                item.getVariationPercent().stripTrailingZeros().toPlainString(),
                isEffectivelyBlocked(item),
                item.isOperating(),
                item.getLastUpdatedAt()
            );
        } finally {
            clearEventCaches();
        }
    }

    private MarketSnapshotItemDTO toSnapshotItem(
        MarketSnapshotProjection item
    ) {
        return new MarketSnapshotItemDTO(
            item.itemId(),
            item.displayName(),
            item.iconKey(),
            Long.toString(item.buyUnitEstimate()),
            Long.toString(item.sellUnitEstimate()),
            item.currency(),
            item.marketPressure(),
            item.marketSegment(),
            item.pressureMagnitude(),
            item.variationPercent(),
            item.blocked(),
            item.operating(),
            item.lastUpdatedAt()
        );
    }

    List<MarketSnapshotProjection> projections(List<MarketItem> items) {
        clearEventCaches();
        try {
            List<MarketSnapshotProjection> projections = new ArrayList<>(
                items.size()
            );
            for (MarketItem item : items) {
                tradePlanner.recomputeDerivedProjections(item);
                MarketTradePlanner.PricingMetadata pricingMetadata =
                    tradePlanner.currentPricingMetadata(item);
                projections.add(
                    new MarketSnapshotProjection(
                    item.getItemId(),
                    item.getCategoryId(),
                    item.getCategoryDisplayName(),
                    item.getCategory() == null
                        ? Integer.MAX_VALUE
                        : item.getCategory().getDisplayOrder(),
                    item.getCategory() == null
                        ? "CHEST"
                        : item.getCategory().getIconKey(),
                    item.getDisplayName(),
                    item.getIconKey(),
                    item.getBuyUnitEstimate(),
                    item.getSellUnitEstimate(),
                    item.getCurrency(),
                    item.getBaseUnitPrice(),
                    item.getMinUnitPrice(),
                    item.getMaxUnitPrice(),
                    item.getSegmentSize(),
                    item.getPriceSensitivity(),
                    item.getSellPricePercentage(),
                    item.getBaseRegenQuantity(),
                    item.getRegenIntervalSeconds(),
                    item.getCurrentStock(),
                    item.getNetPosition(),
                    item.getDriftMultiplierBasisPoints(),
                    item.getDriftRevision(),
                    item.getDriftEvaluatedAt(),
                    pricingMetadata.namedEventInstanceId(),
                    pricingMetadata.eventEffectVersion(),
                    item.getMinNetPosition(),
                    item.getMaxNetPosition(),
                    tradePlanner.pressureSegment(item, item.getNetPosition()),
                    pressureMagnitude(item.getNetPosition()),
                    item.getMarketMomentum(),
                    item
                        .getVariationPercent()
                        .stripTrailingZeros()
                        .toPlainString(),
                    isEffectivelyBlocked(item),
                    item.isOperating(),
                    item.getLastUpdatedAt()
                    )
                );
            }
            projections.sort(
                Comparator
                    .comparingInt(MarketSnapshotProjection::categoryDisplayOrder)
                    .thenComparing(MarketSnapshotProjection::categoryId)
                    .thenComparing(MarketSnapshotProjection::displayName)
                    .thenComparing(MarketSnapshotProjection::itemId)
            );
            return List.copyOf(projections);
        } finally {
            clearEventCaches();
        }
    }

    String snapshotVersion(List<MarketSnapshotProjection> items) {
        return snapshotVersion(items, null);
    }

    String snapshotVersion(
        List<MarketSnapshotProjection> items,
        MarketActiveEventContextDTO activeEvent
    ) {
        StringBuilder payload = new StringBuilder("market");
        for (MarketSnapshotProjection item : items) {
            payload
                .append('|')
                .append(item.itemId())
                .append(':')
                .append(item.currency())
                .append(':')
                .append(item.baseUnitPrice())
                .append(':')
                .append(item.minUnitPrice())
                .append(':')
                .append(item.maxUnitPrice())
                .append(':')
                .append(item.segmentSize())
                .append(':')
                .append(normalizedDecimal(item.priceSensitivity()))
                .append(':')
                .append(normalizedDecimal(item.sellPricePercentage()))
                .append(':')
                .append(item.baseRegenQuantity())
                .append(':')
                .append(item.regenIntervalSeconds())
                .append(':')
                .append(item.marketPressure())
                .append(':')
                .append(item.driftMultiplierBasisPoints())
                .append(':')
                .append(item.driftRevision())
                .append(':')
                .append(item.driftEvaluatedAt())
                .append(':')
                .append(nullableLong(item.namedEventInstanceId()))
                .append(':')
                .append(nullableInteger(item.eventEffectVersion()))
                .append(':')
                .append(nullableLong(item.minNetPosition()))
                .append(':')
                .append(nullableLong(item.maxNetPosition()))
                .append(':')
                .append(item.blocked())
                .append(':')
                .append(item.operating())
                .append(':')
                .append(item.lastUpdatedAt());
        }
        if (activeEvent != null) {
            payload
                .append("|event:")
                .append(activeEvent.name())
                .append(':')
                .append(activeEvent.description())
                .append(':')
                .append(activeEvent.broadScopeHint())
                .append(':')
                .append(activeEvent.temporalLabel());
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                payload.toString().getBytes(StandardCharsets.UTF_8)
            );
            return "market:" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                "SHA-256 digest is not available",
                ex
            );
        }
    }

    private String normalizedDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String nullableLong(Long value) {
        return value == null ? "_" : Long.toString(value);
    }

    private String nullableInteger(Integer value) {
        return value == null ? "_" : Integer.toString(value);
    }

    private long pressureMagnitude(long marketPressure) {
        return marketPressure == Long.MIN_VALUE
            ? Long.MAX_VALUE
            : Math.abs(marketPressure);
    }

    private boolean isEffectivelyBlocked(MarketItem item) {
        return eventBlockingService == null
            ? item.isBlocked()
            : eventBlockingService.isEffectivelyBlocked(item);
    }

    private void clearEventCaches() {
        tradePlanner.clearPricingCache();
        if (eventBlockingService != null) {
            eventBlockingService.clearRequestCache();
        }
    }

    record MarketSnapshotProjection(
        String itemId,
        String categoryId,
        String categoryDisplayName,
        int categoryDisplayOrder,
        String categoryIconKey,
        String displayName,
        String iconKey,
        long buyUnitEstimate,
        long sellUnitEstimate,
        String currency,
        long baseUnitPrice,
        long minUnitPrice,
        long maxUnitPrice,
        long segmentSize,
        BigDecimal priceSensitivity,
        BigDecimal sellPricePercentage,
        long baseRegenQuantity,
        long regenIntervalSeconds,
        long currentStock,
        long marketPressure,
        long driftMultiplierBasisPoints,
        long driftRevision,
        Instant driftEvaluatedAt,
        Long namedEventInstanceId,
        Integer eventEffectVersion,
        Long minNetPosition,
        Long maxNetPosition,
        long marketSegment,
        long pressureMagnitude,
        long marketMomentum,
        String variationPercent,
        boolean blocked,
        boolean operating,
        Instant lastUpdatedAt
    ) {}
}
