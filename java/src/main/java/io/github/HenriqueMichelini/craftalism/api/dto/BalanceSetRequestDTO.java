package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BalanceSetRequestDTO(
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
