package io.github.HenriqueMichelini.craftalism.api.mapper;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BalanceMapper {

    public BalanceResponseDTO toDto(Balance entity) {
        if (entity == null) return null;

        return new BalanceResponseDTO(entity.getUuid(), entity.getAmount());
    }

    public List<BalanceResponseDTO> toDto(List<Balance> entities) {
        if (entities == null) return List.of();

        return entities.stream().map(this::toDto).toList();
    }
}
