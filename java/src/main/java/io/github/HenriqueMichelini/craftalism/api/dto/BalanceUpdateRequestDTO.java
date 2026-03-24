package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record BalanceUpdateRequestDTO(
    @Schema(
        description = "Player's unique identifier (UUID)",
        example = "550e8400-e29b-41d4-a716-446655440000",
        format = "uuid"
    )
    @NotNull(message = "UUID is required")
    UUID uuid,

    @Schema(
        description = "Balance amount (must be greater than 0)",
        example = "1000",
        minimum = "1",
        type = "integer",
        format = "int64"
    )
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    Long amount
) {}
