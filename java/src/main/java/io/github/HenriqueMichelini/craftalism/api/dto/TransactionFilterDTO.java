package io.github.HenriqueMichelini.craftalism.api.dto;

import java.time.Instant;

public record TransactionFilterDTO(
    String fromPlayerUuid,
    String fromPlayerUuidMatch,
    String toPlayerUuid,
    String toPlayerUuidMatch,
    Long minAmount,
    Long maxAmount,
    Instant createdFrom,
    Instant createdTo
) {}
