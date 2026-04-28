package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MissingIdempotencyKeyException extends BusinessException {

    public MissingIdempotencyKeyException() {
        super("Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
    }
}
