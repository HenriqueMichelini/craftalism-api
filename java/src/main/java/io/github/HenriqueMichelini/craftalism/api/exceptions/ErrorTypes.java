package io.github.HenriqueMichelini.craftalism.api.exceptions;

import java.net.URI;

public final class ErrorTypes {

    private ErrorTypes() {}

    public static final URI VALIDATION = URI.create(
        "https://api.craftalism.com/errors/validation"
    );

    public static final URI BUSINESS = URI.create(
        "https://api.craftalism.com/errors/business-rule"
    );

    public static final URI INTERNAL = URI.create(
        "https://api.craftalism.com/errors/internal"
    );
}
