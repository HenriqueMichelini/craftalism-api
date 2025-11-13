package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDTO(Long id, UUID fromUuid, UUID toUuid, Long amount, Instant createdAt) {
}