package io.github.HenriqueMichelini.craftalism.api.dto;

import java.util.UUID;

public record BalanceRequestDTO(UUID uuid, Long amount) {
}