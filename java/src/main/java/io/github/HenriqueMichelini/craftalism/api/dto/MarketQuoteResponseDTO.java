package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record MarketQuoteResponseDTO(
    @Schema(description = "Item identifier", example = "wheat")
    String itemId,

    @Schema(description = "Trade side", example = "BUY")
    MarketSide side,

    @Schema(description = "Quoted quantity", example = "32")
    long quantity,

    @Schema(description = "Authoritative quoted unit price", example = "5")
    String unitPrice,

    @Schema(description = "Authoritative quoted total price", example = "160")
    String totalPrice,

    @Schema(description = "Currency code", example = "coins")
    String currency,

    @Schema(description = "Opaque quote token", example = "7f719e24-d0e3-4ded-b6ff-52c0d1eb7b7f")
    String quoteToken,

    @Schema(description = "Authoritative market-wide snapshot version", example = "market:9c8877")
    String snapshotVersion,

    @Schema(description = "Quote expiration timestamp")
    Instant expiresAt,

    @Schema(description = "Whether the item is blocked", example = "false")
    boolean blocked,

    @Schema(description = "Whether the item is operating", example = "true")
    boolean operating
) {}
