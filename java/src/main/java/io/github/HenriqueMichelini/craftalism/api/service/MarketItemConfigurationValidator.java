package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketItemValidationException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.math.BigDecimal;

final class MarketItemConfigurationValidator {

    void validate(MarketItem item) {
        if (item.getMinUnitPrice() > item.getBaseUnitPrice()) {
            throw new MarketItemValidationException(
                "Minimum unit price must be less than or equal to base unit price"
            );
        }
        if (item.getMaxUnitPrice() < item.getBaseUnitPrice()) {
            throw new MarketItemValidationException(
                "Maximum unit price must be greater than or equal to base unit price"
            );
        }
        if (item.getMinUnitPrice() >= item.getMaxUnitPrice()) {
            throw new MarketItemValidationException(
                "Minimum and maximum unit prices must allow a buy/sell estimate spread"
            );
        }
        if (item.getSellPricePercentage() == null) {
            throw new MarketItemValidationException(
                "Sell price percentage must be provided"
            );
        }
        if (
            item.getSellPricePercentage().signum() <= 0 ||
            item.getSellPricePercentage().compareTo(BigDecimal.ONE) >= 0
        ) {
            throw new MarketItemValidationException(
                "Sell price percentage must be greater than 0 and less than 1"
            );
        }
        if (
            item.getMinNetPosition() != null && item.getMinNetPosition() > 0L
        ) {
            throw new MarketItemValidationException(
                "Minimum net position must be zero or negative"
            );
        }
        if (
            item.getMaxNetPosition() != null && item.getMaxNetPosition() < 0L
        ) {
            throw new MarketItemValidationException(
                "Maximum net position must be zero or positive"
            );
        }
        if (
            item.getMinNetPosition() != null &&
            item.getMaxNetPosition() != null &&
            item.getMinNetPosition() > item.getMaxNetPosition()
        ) {
            throw new MarketItemValidationException(
                "Minimum net position must be less than or equal to maximum net position"
            );
        }
    }
}
