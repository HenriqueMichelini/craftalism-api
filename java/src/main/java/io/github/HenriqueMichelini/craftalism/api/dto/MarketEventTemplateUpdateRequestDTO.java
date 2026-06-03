package io.github.HenriqueMichelini.craftalism.api.dto;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MarketEventTemplateUpdateRequestDTO(
    @NotNull(message = "Rarity is required")
    MarketEventRarity rarity,

    @NotNull(message = "Scope is required")
    MarketEventScope scope,

    @PositiveOrZero(message = "Automatic weight must be zero or positive")
    int automaticWeight,

    boolean automaticEnabled,

    boolean blockingAllowed,

    @Positive(message = "Minimum duration seconds must be positive")
    long minDurationSeconds,

    @Positive(message = "Maximum duration seconds must be positive")
    long maxDurationSeconds,

    @Positive(message = "Minimum effect basis points must be positive")
    int minEffectBasisPoints,

    @Positive(message = "Maximum effect basis points must be positive")
    int maxEffectBasisPoints,

    @Positive(message = "Cooldown seconds must be positive")
    long cooldownSeconds,

    @NotBlank(message = "Player-facing name is required")
    @Size(max = 255, message = "Player-facing name must be at most 255 characters")
    String playerFacingName,

    @NotBlank(message = "Player-facing description is required")
    @Size(max = 2048, message = "Player-facing description must be at most 2048 characters")
    String playerFacingDescription,

    @NotBlank(message = "Broad scope hint is required")
    @Size(max = 255, message = "Broad scope hint must be at most 255 characters")
    String broadScopeHint,

    @NotBlank(message = "Eligible target metadata is required")
    @Size(max = 4096, message = "Eligible target metadata must be at most 4096 characters")
    String eligibleTargetMetadata
) {}
