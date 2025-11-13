package io.github.HenriqueMichelini.craftalism.api.mapper;

import io.github.HenriqueMichelini.craftalism.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public PlayerResponseDTO toDto(Player entity) {
        if (entity == null) return null;

        return new PlayerResponseDTO(
                entity.getUuid(),
                entity.getName()
        );
    }
}