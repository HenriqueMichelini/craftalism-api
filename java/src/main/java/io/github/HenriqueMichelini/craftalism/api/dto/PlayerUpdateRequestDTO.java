package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Data for updating an existing player")
public record PlayerUpdateRequestDTO(
    @Schema(
        description = "Player's in-game name",
        example = "KOLONY_9",
        minLength = 3,
        maxLength = 16
    )
    @NotBlank(message = "Name is required")
    String name
) {}
