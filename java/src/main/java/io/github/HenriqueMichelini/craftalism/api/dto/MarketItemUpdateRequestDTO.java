package io.github.HenriqueMichelini.craftalism.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MarketItemUpdateRequestDTO(
    @NotBlank(message = "Category display name is required")
    @Size(max = 128, message = "Category display name must be at most 128 characters")
    String categoryDisplayName,

    @NotBlank(message = "Icon key is required")
    @Size(max = 64, message = "Icon key must be at most 64 characters")
    String iconKey,

    @NotBlank(message = "Currency is required")
    @Size(max = 32, message = "Currency must be at most 32 characters")
    String currency,

    @NotNull(message = "Base unit price is required")
    @Positive(message = "Base unit price must be positive")
    Long baseUnitPrice,

    @NotNull(message = "Minimum unit price is required")
    @Positive(message = "Minimum unit price must be positive")
    Long minUnitPrice,

    @NotNull(message = "Maximum unit price is required")
    @Positive(message = "Maximum unit price must be positive")
    Long maxUnitPrice,

    @NotNull(message = "Segment size is required")
    @Positive(message = "Segment size must be positive")
    Long segmentSize,

    @NotNull(message = "Price sensitivity is required")
    @DecimalMin(value = "0.0001", message = "Price sensitivity must be positive")
    BigDecimal priceSensitivity,

    @NotNull(message = "Sell price percentage is required")
    @DecimalMin(value = "0.0001", message = "Sell price percentage must be greater than 0")
    @DecimalMax(value = "0.9999", message = "Sell price percentage must be less than 1")
    BigDecimal sellPricePercentage,

    @NotNull(message = "Base regen quantity is required")
    @PositiveOrZero(message = "Base regen quantity must be zero or positive")
    Long baseRegenQuantity,

    @NotNull(message = "Regen interval seconds is required")
    @Positive(message = "Regen interval seconds must be positive")
    Long regenIntervalSeconds,

    @NotNull(message = "Net position is required")
    Long netPosition,

    Long minNetPosition,

    Long maxNetPosition,

    @NotNull(message = "Blocked is required")
    Boolean blocked,

    @NotNull(message = "Operating is required")
    Boolean operating
) {}
