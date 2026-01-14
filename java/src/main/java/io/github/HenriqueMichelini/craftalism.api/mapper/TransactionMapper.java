package io.github.HenriqueMichelini.craftalism.api.mapper;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponseDTO toDto(Transaction entity) {
        return new TransactionResponseDTO(
            entity.getId(),
            entity.getFromPlayerUuid(),
            entity.getToPlayerUuid(),
            entity.getAmount(),
            entity.getCreatedAt()
        );
    }

    public List<TransactionResponseDTO> toDto(List<Transaction> entities) {
        if (entities == null) return List.of();

        return entities.stream().map(this::toDto).toList();
    }
}
