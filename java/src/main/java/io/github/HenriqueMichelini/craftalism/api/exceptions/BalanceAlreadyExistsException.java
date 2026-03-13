package io.github.HenriqueMichelini.craftalism.api.exceptions;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BalanceAlreadyExistsException extends BusinessException {

    public BalanceAlreadyExistsException(UUID uuid) {
        super("Balance already exists for UUID: " + uuid, HttpStatus.CONFLICT);
    }
}
