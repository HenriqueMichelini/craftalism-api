package io.github.HenriqueMichelini.craftalism.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MarketItemCreateRequestDTO(
    @NotBlank(message = "Item ID is required")
    @Size(max = 64, message = "Item ID must be at most 64 characters")
    String itemId,

    @NotBlank(message = "Category ID is required")
    @Size(max = 64, message = "Category ID must be at most 64 characters")
    String categoryId,

    @NotBlank(message = "Category display name is required")
    @Size(max = 128, message = "Category display name must be at most 128 characters")
    String categoryDisplayName,

    @NotBlank(message = "Display name is required")
    @Size(max = 128, message = "Display name must be at most 128 characters")
    String displayName,

    @NotBlank(message = "Icon key is required")
    @Size(max = 64, message = "Icon key must be at most 64 characters")
    String iconKey,

    @NotBlank(message = "Currency is required")
    @Size(max = 32, message = "Currency must be at most 32 characters")
    String currency,

    @Positive(message = "Base unit price must be positive")
    Long baseUnitPrice,

    @Positive(message = "Minimum unit price must be positive")
    Long minUnitPrice,

    @Positive(message = "Maximum unit price must be positive")
    Long maxUnitPrice,

    @Positive(message = "Segment size must be positive")
    Long segmentSize,

    @DecimalMin(value = "0.0001", message = "Price sensitivity must be positive")
    BigDecimal priceSensitivity,

    @DecimalMin(value = "0.0001", message = "Sell price percentage must be greater than 0")
    @DecimalMax(value = "0.9999", message = "Sell price percentage must be less than 1")
    BigDecimal sellPricePercentage,

    @PositiveOrZero(message = "Base regen quantity must be zero or positive")
    Long baseRegenQuantity,

    @Positive(message = "Regen interval seconds must be positive")
    Long regenIntervalSeconds,

    Long netPosition,

    Long minNetPosition,

    Long maxNetPosition,

    @NotNull(message = "Blocked is required")
    Boolean blocked,

    @NotNull(message = "Operating is required")
    Boolean operating
) {}
