package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class PlayerNameAlreadyExistsException extends BusinessException {

    public PlayerNameAlreadyExistsException(String name) {
        super("Player already exists for name: " + name, HttpStatus.CONFLICT);
    }
}
