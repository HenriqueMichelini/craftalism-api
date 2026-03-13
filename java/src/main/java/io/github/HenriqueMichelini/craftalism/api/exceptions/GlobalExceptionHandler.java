package io.github.HenriqueMichelini.craftalism.api.exceptions;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        return buildProblemDetail(ex.getStatus(), ex.getMessage());
    }

    // Validation errors for @Valid DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = buildProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );

        problem.setProperty("errors", errors);
        return problem;
    }

    // path params, request params
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
        ConstraintViolationException ex
    ) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ex.printStackTrace();

        return buildProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected internal server error"
        );
    }

    private ProblemDetail buildProblemDetail(
        HttpStatus status,
        String message
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            status,
            message
        );
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
