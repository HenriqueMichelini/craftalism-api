package io.github.HenriqueMichelini.craftalism.api.exceptions;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BalanceNotFoundException extends BusinessException {

    public BalanceNotFoundException(UUID uuid) {
        super("Balance not found for UUID: " + uuid, HttpStatus.NOT_FOUND);
    }
}
