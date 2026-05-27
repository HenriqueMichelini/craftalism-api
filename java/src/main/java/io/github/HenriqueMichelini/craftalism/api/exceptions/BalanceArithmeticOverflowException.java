package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class BalanceArithmeticOverflowException extends BusinessException {

    public BalanceArithmeticOverflowException() {
        super("Balance amount exceeds supported range.", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
