package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record MarketSnapshotItemDTO(
    @Schema(description = "Item identifier", example = "wheat")
    String itemId,

    @Schema(description = "Item display name", example = "Wheat")
    String displayName,

    @Schema(description = "Item icon key", example = "WHEAT")
    String iconKey,

    @Schema(description = "Display-only buy estimate encoded as a string", example = "5")
    String buyUnitEstimate,

    @Schema(description = "Display-only sell estimate encoded as a string", example = "4")
    String sellUnitEstimate,

    @Schema(description = "Currency code", example = "coins")
    String currency,

    @Schema(description = "Current market stock", example = "1820")
    long currentStock,

    @Schema(description = "Variation percentage string", example = "2.3")
    String variationPercent,

    @Schema(description = "Whether the item is blocked", example = "false")
    boolean blocked,

    @Schema(description = "Whether the item is operating", example = "true")
    boolean operating,

    @Schema(description = "Last update timestamp")
    Instant lastUpdatedAt
) {}
