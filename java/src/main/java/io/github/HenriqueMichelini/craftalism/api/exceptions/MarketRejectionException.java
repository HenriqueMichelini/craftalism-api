package io.github.HenriqueMichelini.craftalism.api.exceptions;

import org.springframework.http.HttpStatus;

public class MarketRejectionException extends RuntimeException {

    private final MarketRejectionCode code;
    private final HttpStatus status;
    private final String snapshotVersion;

    public MarketRejectionException(
        MarketRejectionCode code,
        String message,
        HttpStatus status,
        String snapshotVersion
    ) {
        super(message);
        this.code = code;
        this.status = status;
        this.snapshotVersion = snapshotVersion;
    }

    public MarketRejectionCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getSnapshotVersion() {
        return snapshotVersion;
    }
}
