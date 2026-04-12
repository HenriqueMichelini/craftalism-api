package io.github.HenriqueMichelini.craftalism.api.exceptions;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketRejectionResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MarketExceptionHandler {

    @ExceptionHandler(MarketRejectionException.class)
    public ResponseEntity<MarketRejectionResponseDTO> handleMarketRejection(
        MarketRejectionException ex
    ) {
        return ResponseEntity
            .status(ex.getStatus())
            .body(
                new MarketRejectionResponseDTO(
                    "REJECTED",
                    ex.getCode().name(),
                    ex.getMessage(),
                    ex.getSnapshotVersion()
                )
            );
    }
}
