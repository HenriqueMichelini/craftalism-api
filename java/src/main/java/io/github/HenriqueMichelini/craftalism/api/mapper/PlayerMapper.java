package io.github.HenriqueMichelini.craftalism.api.mapper;

import io.github.HenriqueMichelini.craftalism.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public PlayerResponseDTO toDto(Player entity) {
        if (entity == null) return null;

        return new PlayerResponseDTO(
            entity.getUuid(),
            entity.getName(),
            entity.getCreatedAt()
        );
    }

    public List<PlayerResponseDTO> toDto(List<Player> entities) {
        if (entities == null) return List.of();

        return entities.stream().map(this::toDto).toList();
    }
}
