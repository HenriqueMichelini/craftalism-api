package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketEventTemplateInUseException extends BusinessException {

    public MarketEventTemplateInUseException(String templateId) {
        super(
            "Market event template is referenced and cannot be deleted: " +
            templateId,
            HttpStatus.CONFLICT
        );
    }
}
