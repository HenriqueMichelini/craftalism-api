package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MarketQuoteRequestDTO(
    @Schema(description = "Market item identifier", example = "wheat")
    @NotBlank(message = "Item ID is required")
    String itemId,

    @Schema(description = "Trade side", example = "BUY")
    @NotNull(message = "Side is required")
    MarketSide side,

    @Schema(description = "Requested quantity", example = "32", minimum = "1")
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Long quantity,

    @Schema(
        description = "Opaque market snapshot version used for stale detection",
        example = "market:9c8877"
    )
    @NotBlank(message = "Snapshot version is required")
    String snapshotVersion,

    @Schema(
        description = "Bukkit player UUID supplied by the trusted Minecraft server client",
        example = "220e8400-e29b-41d4-a716-446655440000"
    )
    String playerUuid
) {}
