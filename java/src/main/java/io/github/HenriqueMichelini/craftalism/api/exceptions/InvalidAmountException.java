package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidAmountException extends BusinessException {

    public InvalidAmountException() {
        super("Invalid amount.", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
