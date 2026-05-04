package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotCategoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotItemDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MarketSnapshotProjector {

    private final MarketTradePlanner tradePlanner;

    MarketSnapshotProjector(MarketTradePlanner tradePlanner) {
        this.tradePlanner = tradePlanner;
    }

    MarketSnapshotResponseDTO response(
        List<MarketSnapshotProjection> projections,
        String snapshotVersion
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
                        new ArrayList<>()
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

    MarketSnapshotItemDTO toSnapshotItem(MarketItem item) {
        tradePlanner.recomputeDerivedProjections(item);
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
            item.currentStock(),
            item.variationPercent(),
            item.blocked(),
            item.operating(),
            item.lastUpdatedAt()
        );
    }

    List<MarketSnapshotProjection> projections(List<MarketItem> items) {
        List<MarketSnapshotProjection> projections = new ArrayList<>(
            items.size()
        );
        for (MarketItem item : items) {
            tradePlanner.recomputeDerivedProjections(item);
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
                    item
                        .getVariationPercent()
                        .stripTrailingZeros()
                        .toPlainString(),
                    item.isBlocked(),
                    item.isOperating(),
                    item.getLastUpdatedAt(),
                    tradePlanner
                        .sortedSegments(item)
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

    String snapshotVersion(List<MarketSnapshotProjection> items) {
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

    record MarketSegmentProjection(
        long segmentIndex,
        long maxCapacity,
        long remainingCapacity,
        long unitPrice
    ) {}

    record MarketSnapshotProjection(
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
}
