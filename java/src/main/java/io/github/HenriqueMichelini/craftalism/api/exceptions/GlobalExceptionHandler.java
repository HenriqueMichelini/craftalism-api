package io.github.HenriqueMichelini.craftalism.api.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(
        BusinessException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = buildProblemDetail(
            ex.getStatus(),
            ex.getMessage(),
            request
        );

        problem.setType(ErrorTypes.BUSINESS);

        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        HttpServletRequest servletRequest = (
            (ServletWebRequest) request
        ).getRequest();

        ProblemDetail problem = buildProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            servletRequest
        );

        problem.setType(ErrorTypes.VALIDATION);
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = buildProblemDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request
        );

        problem.setType(ErrorTypes.VALIDATION);

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unexpected server error", ex);

        ProblemDetail problem = buildProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected internal server error",
            request
        );

        problem.setType(ErrorTypes.INTERNAL);

        return problem;
    }

    private ProblemDetail buildProblemDetail(
        HttpStatus status,
        String message,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            status,
            message
        );

        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());

        return problem;
    }
}
