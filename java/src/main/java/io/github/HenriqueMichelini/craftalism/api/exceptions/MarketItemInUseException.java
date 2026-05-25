package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketItemInUseException extends BusinessException {

    public MarketItemInUseException(String itemId) {
        super("Market item is referenced and cannot be deleted: " + itemId, HttpStatus.CONFLICT);
    }
}
