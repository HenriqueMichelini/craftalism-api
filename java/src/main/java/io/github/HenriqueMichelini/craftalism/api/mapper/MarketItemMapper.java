package io.github.HenriqueMichelini.craftalism.api.mapper;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketItemResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MarketItemMapper {

    public MarketItemResponseDTO toDto(MarketItem item) {
        if (item == null) return null;

        return new MarketItemResponseDTO(
            item.getItemId(),
            item.getCategoryId(),
            item.getCategoryDisplayName(),
            item.getDisplayName(),
            item.getIconKey(),
            item.getBuyUnitEstimate(),
            item.getSellUnitEstimate(),
            item.getCurrency(),
            item.getCurrentStock(),
            item.getVariationPercent(),
            item.isBlocked(),
            item.isOperating(),
            item.getLastUpdatedAt(),
            item.getMarketMomentum(),
            item.getBaseUnitPrice(),
            item.getMinUnitPrice(),
            item.getMaxUnitPrice(),
            item.getSegmentSize(),
            item.getPriceSensitivity(),
            item.getSellPricePercentage(),
            item.getBaseRegenQuantity(),
            item.getRegenIntervalSeconds(),
            item.getNetPosition(),
            item.getMinNetPosition(),
            item.getMaxNetPosition()
        );
    }

    public List<MarketItemResponseDTO> toDto(List<MarketItem> items) {
        if (items == null) return List.of();

        return items.stream().map(this::toDto).toList();
    }
}
