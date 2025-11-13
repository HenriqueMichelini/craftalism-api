package io.github.HenriqueMichelini.craftalism.api.mapper;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import org.springframework.stereotype.Component;

@Component
public class BalanceMapper {

    public BalanceResponseDTO toDto(Balance entity) {
        if (entity == null) return null;

        return new BalanceResponseDTO(
                entity.getUuid(),
                entity.getAmount()
        );
    }
}