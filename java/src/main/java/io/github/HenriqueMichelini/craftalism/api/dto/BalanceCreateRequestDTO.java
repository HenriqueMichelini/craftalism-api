package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record BalanceCreateRequestDTO(
    @Schema(
        description = "Player's unique identifier (UUID)",
        example = "550e8400-e29b-41d4-a716-446655440000",
        format = "uuid"
    )
    @NotNull(message = "UUID is required")
    UUID uuid,

    @Schema(
        description = "Balance amount (must be non-negative)",
        example = "1000",
        minimum = "0",
        type = "integer",
        format = "int64"
    )
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must be zero or positive")
    Long amount
) {}
