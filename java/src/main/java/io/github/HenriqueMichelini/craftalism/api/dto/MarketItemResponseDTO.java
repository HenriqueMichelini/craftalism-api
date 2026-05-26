package io.github.HenriqueMichelini.craftalism.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketItemResponseDTO(
    String itemId,
    String categoryId,
    String categoryDisplayName,
    String displayName,
    String iconKey,
    long buyUnitEstimate,
    long sellUnitEstimate,
    String currency,
    long currentStock,
    BigDecimal variationPercent,
    boolean blocked,
    boolean operating,
    Instant lastUpdatedAt,
    long marketMomentum,
    long baseUnitPrice,
    long minUnitPrice,
    long maxUnitPrice,
    long segmentSize,
    BigDecimal priceSensitivity,
    BigDecimal sellPricePercentage,
    long baseRegenQuantity,
    long regenIntervalSeconds,
    long netPosition,
    Long minNetPosition,
    Long maxNetPosition
) {}
