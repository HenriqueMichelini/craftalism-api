package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MarketActiveEventContextDTO(
    @Schema(description = "Player-facing active event name")
    String name,

    @Schema(description = "Player-facing event narrative")
    String description,

    @Schema(description = "Broad affected market scope")
    String broadScopeHint,

    @Schema(description = "Rough temporal state, without an exact countdown")
    String temporalLabel
) {}
