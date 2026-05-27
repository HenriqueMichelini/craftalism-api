package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketCategoryInUseException extends BusinessException {

    public MarketCategoryInUseException(String categoryId) {
        super(
            "Market category is referenced and cannot be deleted: " +
            categoryId,
            HttpStatus.CONFLICT
        );
    }
}
