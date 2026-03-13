package io.github.HenriqueMichelini.craftalism.api.exceptions;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PlayerAlreadyExistsException extends BusinessException {

    public PlayerAlreadyExistsException(UUID uuid) {
        super("Player already exists for UUID: " + uuid, HttpStatus.CONFLICT);
    }
}
