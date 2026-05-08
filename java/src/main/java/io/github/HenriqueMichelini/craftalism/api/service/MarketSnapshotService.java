package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotItemDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class MarketSnapshotService {

    private final MarketReadService marketReadService;
    private final MarketSnapshotProjector snapshotProjector;

    MarketSnapshotService(
        MarketReadService marketReadService,
        MarketSnapshotProjector snapshotProjector
    ) {
        this.marketReadService = marketReadService;
        this.snapshotProjector = snapshotProjector;
    }

    MarketSnapshotResponseDTO getSnapshot() {
        long totalStartNanos = System.nanoTime();

        MarketReadService.MarketReadState readState =
            marketReadService.regeneratedItems();

        long projectionStartNanos = System.nanoTime();
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections =
            snapshotProjector.projections(readState.items());
        long projectionBuildNanos = System.nanoTime() - projectionStartNanos;

        long hashStartNanos = System.nanoTime();
        String snapshotVersion = snapshotProjector.snapshotVersion(projections);
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

    CurrentSnapshot currentSnapshot() {
        MarketReadService.MarketReadState readState =
            marketReadService.regeneratedItems();
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections =
            snapshotProjector.projections(readState.items());
        return new CurrentSnapshot(
            readState.items(),
            snapshotProjector.snapshotVersion(projections)
        );
    }

    String currentSnapshotVersion() {
        return currentSnapshot().snapshotVersion();
    }

    MarketSnapshotItemDTO toSnapshotItem(MarketItem item) {
        return snapshotProjector.toSnapshotItem(item);
    }

    private void logSnapshotTiming(
        MarketReadService.MarketReadState readState,
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections,
        long projectionBuildNanos,
        long hashNanos,
        long totalNanos
    ) {
        log.info(
            "market.snapshot.timing totalMs={} fetchMs={} regenerationMs={} projectionBuildMs={} hashMs={} items={} regeneratedItems={}",
            nanosToMillis(totalNanos),
            nanosToMillis(readState.fetchNanos()),
            nanosToMillis(readState.regenerationNanos()),
            nanosToMillis(projectionBuildNanos),
            nanosToMillis(hashNanos),
            projections.size(),
            readState.regeneratedItemCount()
        );
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    record CurrentSnapshot(List<MarketItem> items, String snapshotVersion) {}
}
