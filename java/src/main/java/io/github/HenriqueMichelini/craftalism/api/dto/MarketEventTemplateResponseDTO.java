package io.github.HenriqueMichelini.craftalism.api.dto;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import java.time.Instant;

public record MarketEventTemplateResponseDTO(
    String templateId,
    MarketEventRarity rarity,
    MarketEventScope scope,
    int automaticWeight,
    boolean automaticEnabled,
    boolean blockingAllowed,
    long minDurationSeconds,
    long maxDurationSeconds,
    int minEffectBasisPoints,
    int maxEffectBasisPoints,
    String effectDirection,
    long cooldownSeconds,
    String playerFacingName,
    String playerFacingDescription,
    String broadScopeHint,
    String eligibleTargetMetadata,
    Instant createdAt,
    Instant updatedAt
) {}
