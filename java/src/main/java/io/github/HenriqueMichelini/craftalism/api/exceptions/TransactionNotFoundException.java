package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends BusinessException {

    public TransactionNotFoundException(Long id) {
        super("Transaction not found for ID: " + id, HttpStatus.NOT_FOUND);
    }
}
