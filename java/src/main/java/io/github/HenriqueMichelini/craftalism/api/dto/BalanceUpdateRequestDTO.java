package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BalanceUpdateRequestDTO(
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
