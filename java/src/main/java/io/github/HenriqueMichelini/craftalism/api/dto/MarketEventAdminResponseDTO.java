package io.github.HenriqueMichelini.craftalism.api.dto;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventEndReason;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import java.time.Instant;

public record MarketEventAdminResponseDTO(
    Long id,
    String templateId,
    MarketEventSource source,
    MarketEventRarity rarity,
    MarketEventScope scope,
    String selectedCategoryId,
    String selectedItemIds,
    int effectBasisPoints,
    int effectVersion,
    boolean blocking,
    Instant startedAt,
    Instant endsAt,
    MarketEventStatus status,
    MarketEventEndReason endReason,
    String actor,
    String auditMetadata,
    Instant createdAt,
    Instant updatedAt
) {}
