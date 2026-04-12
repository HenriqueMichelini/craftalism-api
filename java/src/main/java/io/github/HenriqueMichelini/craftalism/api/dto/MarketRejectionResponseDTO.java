package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MarketRejectionResponseDTO(
    @Schema(description = "Business rejection status", example = "REJECTED")
    String status,

    @Schema(description = "Stable machine-readable rejection code", example = "STALE_QUOTE")
    String code,

    @Schema(description = "Human-readable rejection message", example = "Quote is no longer valid.")
    String message,

    @Schema(description = "Latest market-wide snapshot version", example = "market:9c8877")
    String snapshotVersion
) {}
