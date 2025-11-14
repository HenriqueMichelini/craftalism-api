package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PlayerResponseDTO(
        @Schema(description = "Player unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID uuid,

        @Schema(description = "Player name", example = "KOLONY_9",  minLength = 3, maxLength = 16)
        @NotBlank(message = "Name is required")
        String name
    ) {
}