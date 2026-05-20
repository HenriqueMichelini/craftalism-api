package io.github.HenriqueMichelini.craftalism.api.exceptions;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PlayerInUseException extends BusinessException {

    public PlayerInUseException(UUID uuid) {
        super("Player is referenced and cannot be deleted: " + uuid, HttpStatus.CONFLICT);
    }
}
