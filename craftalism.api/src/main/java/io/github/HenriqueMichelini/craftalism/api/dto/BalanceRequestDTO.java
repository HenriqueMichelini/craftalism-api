package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Data for creating or updating a balance")
public record BalanceRequestDTO(
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
        @Min(value = 0, message = "Amount must be non-negative")
        Long amount
) {}