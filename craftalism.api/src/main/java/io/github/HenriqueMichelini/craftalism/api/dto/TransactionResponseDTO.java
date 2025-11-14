package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDTO(
        @Schema(
                description = "Transaction unique identifier",
                example = "123",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Long id,

        @Schema(
                description = "Sender's UUID (who sent the money)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid"
        )
        UUID fromUuid,

        @Schema(
                description = "Receiver's UUID (who received the money)",
                example = "550e8400-e29b-41d4-a716-446655440001",
                format = "uuid"
        )
        UUID toUuid,

        @Schema(
                description = "Transaction amount transferred",
                example = "1000",
                minimum = "1",
                type = "integer",
                format = "int64"
        )
        Long amount,

        @Schema(
                description = "Timestamp when the transaction was created",
                example = "2024-11-14T15:30:00Z",
                type = "string",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        Instant createdAt
) {}