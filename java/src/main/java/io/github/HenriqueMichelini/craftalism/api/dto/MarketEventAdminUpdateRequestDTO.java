package io.github.HenriqueMichelini.craftalism.api.dto;

import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record MarketEventAdminUpdateRequestDTO(
    Integer effectBasisPoints,
    Boolean blocking,
    @Positive Long durationSeconds,
    Instant endsAt,
    String reason
) {}
