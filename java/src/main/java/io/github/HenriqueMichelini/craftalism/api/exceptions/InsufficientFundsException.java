package io.github.HenriqueMichelini.craftalism.api.exceptions;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(UUID uuid, Long amount) {
        super(
            "Insufficient funds for uuid: " + uuid + " | amount: " + amount,
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}
