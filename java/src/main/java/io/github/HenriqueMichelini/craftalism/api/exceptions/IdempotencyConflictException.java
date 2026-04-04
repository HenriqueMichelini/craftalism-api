package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException(String idempotencyKey) {
        super(
            "Idempotency key conflict for key: " + idempotencyKey,
            HttpStatus.CONFLICT
        );
    }
}
