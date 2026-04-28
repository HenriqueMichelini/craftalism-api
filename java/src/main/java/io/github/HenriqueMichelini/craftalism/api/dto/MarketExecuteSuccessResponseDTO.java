package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MarketExecuteSuccessResponseDTO(
    @Schema(description = "Execution status", example = "SUCCESS")
    String status,

    @Schema(description = "Item identifier", example = "wheat")
    String itemId,

    @Schema(description = "Trade side", example = "BUY")
    MarketSide side,

    @Schema(description = "Executed quantity", example = "32")
    long executedQuantity,

    @Schema(description = "Authoritative executed unit price", example = "5")
    String unitPrice,

    @Schema(description = "Authoritative executed total price", example = "160")
    String totalPrice,

    @Schema(description = "Currency code", example = "coins")
    String currency,

    @Schema(description = "Snapshot version after execution", example = "market:9c8877")
    String snapshotVersion,

    @Schema(description = "Updated item snapshot after the execution")
    MarketSnapshotItemDTO updatedItem
) {}
