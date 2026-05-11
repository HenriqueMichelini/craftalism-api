package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketTradeHistoryNotFoundException extends BusinessException {

    public MarketTradeHistoryNotFoundException(Long id) {
        super("Market trade history not found for ID: " + id, HttpStatus.NOT_FOUND);
    }
}
