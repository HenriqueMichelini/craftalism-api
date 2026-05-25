package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketItemNotFoundException extends BusinessException {

    public MarketItemNotFoundException(String itemId) {
        super("Market item not found for item ID: " + itemId, HttpStatus.NOT_FOUND);
    }
}
