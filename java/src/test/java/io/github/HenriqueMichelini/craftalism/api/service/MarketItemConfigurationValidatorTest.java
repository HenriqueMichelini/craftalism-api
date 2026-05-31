package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketItemValidationException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.math.BigDecimal;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class MarketItemConfigurationValidatorTest {

    private final MarketItemConfigurationValidator validator =
        new MarketItemConfigurationValidator();

    @Test
    void validConfiguration_passesValidation() {
        assertDoesNotThrow(() -> validator.validate(validItem()));
    }

    @Test
    void minimumUnitPriceAboveBaseUnitPrice_isRejected() {
        assertInvalid(
            item -> item.setMinUnitPrice(101L),
            "Minimum unit price must be less than or equal to base unit price"
        );
    }

    @Test
    void maximumUnitPriceBelowBaseUnitPrice_isRejected() {
        assertInvalid(
            item -> item.setMaxUnitPrice(99L),
            "Maximum unit price must be greater than or equal to base unit price"
        );
    }

    @Test
    void unitPriceRangeWithoutSpread_isRejected() {
        assertInvalid(
            item -> {
                item.setMinUnitPrice(100L);
                item.setMaxUnitPrice(100L);
            },
            "Minimum and maximum unit prices must allow a buy/sell estimate spread"
        );
    }

    @Test
    void missingSellPricePercentage_isRejected() {
        assertInvalid(
            item -> item.setSellPricePercentage(null),
            "Sell price percentage must be provided"
        );
    }

    @Test
    void nonPositiveSellPricePercentage_isRejected() {
        assertInvalid(
            item -> item.setSellPricePercentage(BigDecimal.ZERO),
            "Sell price percentage must be greater than 0 and less than 1"
        );
    }

    @Test
    void sellPricePercentageEqualToOne_isRejected() {
        assertInvalid(
            item -> item.setSellPricePercentage(BigDecimal.ONE),
            "Sell price percentage must be greater than 0 and less than 1"
        );
    }

    @Test
    void positiveMinimumNetPosition_isRejected() {
        assertInvalid(
            item -> item.setMinNetPosition(1L),
            "Minimum net position must be zero or negative"
        );
    }

    @Test
    void negativeMaximumNetPosition_isRejected() {
        assertInvalid(
            item -> item.setMaxNetPosition(-1L),
            "Maximum net position must be zero or positive"
        );
    }

    @Test
    void invertedNetPositionRange_isRejectedDefensively() {
        MarketItem item = mock(MarketItem.class);
        when(item.getMinUnitPrice()).thenReturn(50L);
        when(item.getBaseUnitPrice()).thenReturn(100L);
        when(item.getMaxUnitPrice()).thenReturn(300L);
        when(item.getSellPricePercentage()).thenReturn(new BigDecimal("0.7000"));
        when(item.getMinNetPosition()).thenReturn(-1L);
        when(item.getMaxNetPosition()).thenReturn(1L, 1L, -2L, -2L);

        MarketItemValidationException exception = assertThrows(
            MarketItemValidationException.class,
            () -> validator.validate(item)
        );

        assertEquals(
            "Minimum net position must be less than or equal to maximum net position",
            exception.getMessage()
        );
    }

    private void assertInvalid(
        Consumer<MarketItem> mutation,
        String expectedMessage
    ) {
        MarketItem item = validItem();
        mutation.accept(item);

        MarketItemValidationException exception = assertThrows(
            MarketItemValidationException.class,
            () -> validator.validate(item)
        );

        assertEquals(expectedMessage, exception.getMessage());
    }

    private static MarketItem validItem() {
        MarketItem item = new MarketItem();
        item.setBaseUnitPrice(100L);
        item.setMinUnitPrice(50L);
        item.setMaxUnitPrice(300L);
        item.setSellPricePercentage(new BigDecimal("0.7000"));
        return item;
    }
}
