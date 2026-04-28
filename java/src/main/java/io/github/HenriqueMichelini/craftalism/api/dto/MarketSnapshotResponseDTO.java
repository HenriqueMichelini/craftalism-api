package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record MarketSnapshotResponseDTO(
    @Schema(
        description = "Opaque market-wide stale detection token",
        example = "market:9c8877"
    )
    String snapshotVersion,

    @Schema(description = "Snapshot generation timestamp")
    Instant generatedAt,

    @Schema(description = "Grouped market categories")
    List<MarketSnapshotCategoryDTO> categories
) {}
