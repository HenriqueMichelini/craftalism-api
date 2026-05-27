package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;

public record MarketCategoryResponseDTO(
    String categoryId,
    String displayName,
    String iconKey,
    int displayOrder,
    Instant createdAt,
    Instant updatedAt
) {}
