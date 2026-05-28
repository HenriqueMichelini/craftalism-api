package io.github.HenriqueMichelini.craftalism.api.dto;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record MarketEventAdminCreateRequestDTO(
    @NotBlank String templateId,
    MarketEventScope scope,
    String selectedCategoryId,
    String selectedItemIds,
    Integer effectBasisPoints,
    Boolean blocking,
    @Positive Long durationSeconds,
    String reason
) {}
