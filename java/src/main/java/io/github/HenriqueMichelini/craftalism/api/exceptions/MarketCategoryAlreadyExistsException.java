package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketCategoryAlreadyExistsException extends BusinessException {

    public MarketCategoryAlreadyExistsException(String categoryId) {
        super(
            "Market category already exists for category ID: " + categoryId,
            HttpStatus.CONFLICT
        );
    }
}
