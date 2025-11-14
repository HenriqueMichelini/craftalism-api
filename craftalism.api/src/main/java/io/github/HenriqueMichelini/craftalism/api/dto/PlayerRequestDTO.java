package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Data for creating a new player")
public record PlayerRequestDTO(
        @Schema(
                description = "Player's unique identifier (UUID)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid"
        )
        @NotNull(message = "UUID is required")
        UUID uuid,

        @Schema(
                description = "Player's in-game name",
                example = "KOLONY_9",
                minLength = 1,
                maxLength = 16
        )
        @NotBlank(message = "Name is required")
        String name
) {}