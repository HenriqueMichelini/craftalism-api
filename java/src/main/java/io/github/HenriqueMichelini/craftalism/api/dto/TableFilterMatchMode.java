package io.github.HenriqueMichelini.craftalism.api.dto;

import io.github.HenriqueMichelini.craftalism.api.exceptions.TableFilterValidationException;
import java.util.Locale;
import java.util.Set;

public enum TableFilterMatchMode {
    CONTAINS,
    EXACT;

    public static final String DEFAULT = "contains";

    public static TableFilterMatchMode fromQueryValue(String value) {
        String effectiveValue = value == null || value.isBlank()
            ? DEFAULT
            : value.trim();
        return switch (effectiveValue.toLowerCase(Locale.ROOT)) {
            case "contains" -> CONTAINS;
            case "exact" -> EXACT;
            default -> throw new TableFilterValidationException(
                "match mode must be one of " + Set.of("contains", "exact")
            );
        };
    }
}
