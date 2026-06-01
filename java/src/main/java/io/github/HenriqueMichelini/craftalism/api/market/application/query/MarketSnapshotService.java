package io.github.HenriqueMichelini.craftalism.api.market.application.query;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketActiveEventContextDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotItemDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.market.domain.snapshot.MarketSnapshotProjector;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MarketSnapshotService {

    private final MarketSnapshotStateLoader marketSnapshotStateLoader;
    private final MarketSnapshotProjector snapshotProjector;
    private final MarketEventPublicContextService eventPublicContextService;

    public MarketSnapshotService(
        MarketSnapshotStateLoader marketSnapshotStateLoader,
        MarketSnapshotProjector snapshotProjector
    ) {
        this(marketSnapshotStateLoader, snapshotProjector, null);
    }

    public MarketSnapshotService(
        MarketSnapshotStateLoader marketSnapshotStateLoader,
        MarketSnapshotProjector snapshotProjector,
        MarketEventPublicContextService eventPublicContextService
    ) {
        this.marketSnapshotStateLoader = marketSnapshotStateLoader;
        this.snapshotProjector = snapshotProjector;
        this.eventPublicContextService = eventPublicContextService;
    }

    public MarketSnapshotResponseDTO getSnapshot() {
        long totalStartNanos = System.nanoTime();

        MarketSnapshotStateLoader.MarketSnapshotState snapshotState =
            marketSnapshotStateLoader.regeneratedItems();

        long projectionStartNanos = System.nanoTime();
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections =
            snapshotProjector.projections(snapshotState.items());
        long projectionBuildNanos = System.nanoTime() - projectionStartNanos;
        MarketActiveEventContextDTO activeEvent = activeEventContext()
            .orElse(null);

        long hashStartNanos = System.nanoTime();
        String snapshotVersion = snapshotProjector.snapshotVersion(
            projections,
            activeEvent
        );
        long hashNanos = System.nanoTime() - hashStartNanos;

        long totalNanos = System.nanoTime() - totalStartNanos;
        logSnapshotTiming(
            snapshotState,
            projections,
            projectionBuildNanos,
            hashNanos,
            totalNanos
        );

        return snapshotProjector.response(
            projections,
            snapshotVersion,
            activeEvent
        );
    }

    public CurrentSnapshot currentSnapshot() {
        MarketSnapshotStateLoader.MarketSnapshotState snapshotState =
            marketSnapshotStateLoader.regeneratedItems();
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections =
            snapshotProjector.projections(snapshotState.items());
        MarketActiveEventContextDTO activeEvent = activeEventContext()
            .orElse(null);
        return new CurrentSnapshot(
            snapshotState.items(),
            snapshotProjector.snapshotVersion(projections, activeEvent)
        );
    }

    public String currentSnapshotVersion() {
        return currentSnapshot().snapshotVersion();
    }

    public MarketSnapshotItemDTO toSnapshotItem(MarketItem item) {
        return snapshotProjector.toSnapshotItem(item);
    }

    private void logSnapshotTiming(
        MarketSnapshotStateLoader.MarketSnapshotState snapshotState,
        List<MarketSnapshotProjector.MarketSnapshotProjection> projections,
        long projectionBuildNanos,
        long hashNanos,
        long totalNanos
    ) {
        log.info(
            "market.snapshot.timing totalMs={} fetchMs={} regenerationMs={} projectionBuildMs={} hashMs={} items={} regeneratedItems={}",
            nanosToMillis(totalNanos),
            nanosToMillis(snapshotState.fetchNanos()),
            nanosToMillis(snapshotState.regenerationNanos()),
            nanosToMillis(projectionBuildNanos),
            nanosToMillis(hashNanos),
            projections.size(),
            snapshotState.regeneratedItemCount()
        );
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private Optional<MarketActiveEventContextDTO> activeEventContext() {
        if (eventPublicContextService == null) {
            return Optional.empty();
        }
        return eventPublicContextService.activeContext(Instant.now());
    }

    public record CurrentSnapshot(List<MarketItem> items, String snapshotVersion) {}
}
