package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketItemManagedByCatalogException extends BusinessException {

    public MarketItemManagedByCatalogException(String itemId) {
        super("Market item is managed by the default catalog and cannot be deleted: " + itemId, HttpStatus.CONFLICT);
    }
}
