package io.github.HenriqueMichelini.craftalism.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Fuzzy active named event context when one is active")
    MarketActiveEventContextDTO activeEvent,

    @Schema(description = "Grouped market categories")
    List<MarketSnapshotCategoryDTO> categories
) {}
