package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record BalanceResponseDTO(
        @Schema(description = "Balance unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID uuid,

        @Schema(description = "Current balance amount", example = "1000", minimum = "0")
        Long amount
    ) {
}