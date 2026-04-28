package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record BalanceTransferRequestDTO(
    @Schema(
        description = "Sender's UUID",
        example = "550e8400-e29b-41d4-a716-446655440000",
        format = "uuid"
    )
    @NotNull(message = "Sender UUID is required")
    UUID fromPlayerUuid,

    @Schema(
        description = "Receiver's UUID",
        example = "550e8400-e29b-41d4-a716-446655440001",
        format = "uuid"
    )
    @NotNull(message = "Receiver UUID is required")
    UUID toPlayerUuid,

    @Schema(
        description = "Amount to transfer",
        example = "1000",
        minimum = "1",
        type = "integer",
        format = "int64"
    )
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    Long amount
) {}
