package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MarketTradeHistoryDTO(
    Long id,
    UUID playerUuid,
    String itemId,
    MarketSide side,
    long quantity,
    String unitPrice,
    String totalPrice,
    String currency,
    String snapshotVersion,
    Instant executedAt
) {}
