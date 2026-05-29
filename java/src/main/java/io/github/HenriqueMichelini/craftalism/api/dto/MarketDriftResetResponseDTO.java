package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;

public record MarketDriftResetResponseDTO(
    int resetItemCount,
    long driftMultiplierBasisPoints,
    Instant driftEvaluatedAt
) {}
