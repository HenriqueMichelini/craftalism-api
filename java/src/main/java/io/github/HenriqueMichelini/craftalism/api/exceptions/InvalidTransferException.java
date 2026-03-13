package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTransferException extends BusinessException {

    public InvalidTransferException() {
        super(
            "Source and destination accounts must be different.",
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}
