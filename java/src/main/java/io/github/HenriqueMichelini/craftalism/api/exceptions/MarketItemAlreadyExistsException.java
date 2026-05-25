package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketItemAlreadyExistsException extends BusinessException {

    public MarketItemAlreadyExistsException(String itemId) {
        super("Market item already exists for item ID: " + itemId, HttpStatus.CONFLICT);
    }
}
