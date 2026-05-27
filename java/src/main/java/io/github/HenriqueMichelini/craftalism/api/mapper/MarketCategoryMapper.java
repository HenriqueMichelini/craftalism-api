package io.github.HenriqueMichelini.craftalism.api.mapper;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketCategoryResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MarketCategoryMapper {

    public MarketCategoryResponseDTO toDto(MarketCategory category) {
        if (category == null) return null;

        return new MarketCategoryResponseDTO(
            category.getCategoryId(),
            category.getDisplayName(),
            category.getIconKey(),
            category.getDisplayOrder(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }

    public List<MarketCategoryResponseDTO> toDto(List<MarketCategory> categories) {
        if (categories == null) return List.of();

        return categories.stream().map(this::toDto).toList();
    }
}
