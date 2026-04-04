package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TransferIncidentResponseDTO(
    Long id,
    String incidentType,
    UUID fromPlayerUuid,
    UUID toPlayerUuid,
    String idempotencyKey,
    String reason,
    String metadata,
    Instant createdAt
) {}
