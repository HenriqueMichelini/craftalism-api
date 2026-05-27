package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketCategoryNotFoundException extends BusinessException {

    public MarketCategoryNotFoundException(String categoryId) {
        super(
            "Market category not found for category ID: " + categoryId,
            HttpStatus.NOT_FOUND
        );
    }
}
