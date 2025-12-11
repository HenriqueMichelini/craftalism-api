package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException (UUID uuid, Long amount) {
        super("Insufficient funds for uuid: " + uuid + " | amount: " + amount);
    }
}
