package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MarketSnapshotCategoryDTO(
    @Schema(description = "Category identifier", example = "farming")
    String categoryId,

    @Schema(description = "Category display name", example = "Farming")
    String displayName,

    @Schema(description = "Items inside the category")
    List<MarketSnapshotItemDTO> items
) {}
