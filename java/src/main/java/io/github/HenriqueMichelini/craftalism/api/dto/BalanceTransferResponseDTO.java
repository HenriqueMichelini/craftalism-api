package io.github.HenriqueMichelini.craftalism.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BalanceTransferResponseDTO(
    @Schema(description = "Persisted transaction data")
    TransactionResponseDTO transaction,

    @Schema(
        description = "True when the response was returned from an idempotent replay",
        example = "false"
    )
    boolean idempotentReplay
) {}
