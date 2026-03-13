package io.github.HenriqueMichelini.craftalism.api.exceptions;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PlayerNotFoundException extends BusinessException {

    public PlayerNotFoundException(UUID uuid) {
        super("Player not found for UUID: " + uuid, HttpStatus.NOT_FOUND);
    }

    public PlayerNotFoundException(String name) {
        super("Player not found for name: " + name, HttpStatus.NOT_FOUND);
    }
}
