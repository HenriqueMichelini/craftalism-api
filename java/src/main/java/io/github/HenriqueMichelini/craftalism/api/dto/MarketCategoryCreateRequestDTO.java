package io.github.HenriqueMichelini.craftalism.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarketCategoryCreateRequestDTO(
    @NotBlank(message = "Category ID is required")
    @Size(max = 64, message = "Category ID must be at most 64 characters")
    String categoryId,

    @NotBlank(message = "Display name is required")
    @Size(max = 128, message = "Display name must be at most 128 characters")
    String displayName,

    @NotBlank(message = "Icon key is required")
    @Size(max = 64, message = "Icon key must be at most 64 characters")
    String iconKey,

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be zero or positive")
    Integer displayOrder
) {}
