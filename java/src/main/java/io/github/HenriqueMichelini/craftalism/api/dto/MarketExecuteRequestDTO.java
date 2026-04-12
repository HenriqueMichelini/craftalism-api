package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MarketExecuteRequestDTO(
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
        description = "Opaque quote token returned by the quote endpoint",
        example = "7f719e24-d0e3-4ded-b6ff-52c0d1eb7b7f"
    )
    @NotBlank(message = "Quote token is required")
    String quoteToken,

    @Schema(
        description = "Opaque market snapshot version associated with the quote",
        example = "market:9c8877"
    )
    @NotBlank(message = "Snapshot version is required")
    String snapshotVersion
) {}
