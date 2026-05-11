package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MarketTradeHistoryFilterDTO(
    UUID playerUuid,
    String itemId,
    MarketSide side,
    Instant executedFrom,
    Instant executedTo
) {}
