package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;

public record MarketTradeHistoryFilterDTO(
    String playerUuid,
    String playerUuidMatch,
    String itemId,
    String itemIdMatch,
    MarketSide side,
    Long minTotalPrice,
    Long maxTotalPrice,
    Instant executedFrom,
    Instant executedTo
) {}
