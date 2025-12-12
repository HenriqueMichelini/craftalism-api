package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(UUID uuid) {
        super("Player not found for UUID: " + uuid);
    }

    public PlayerNotFoundException(String name) {
        super("Player not found for name: " + name);
    }
}