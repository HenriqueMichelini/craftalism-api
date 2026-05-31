package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TableFilterMatchMode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TableFilterValidationException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class TableFilterValidation {

    private TableFilterValidation() {}

    static void validateNonNegativeRange(
        Long min,
        String minProperty,
        Long max,
        String maxProperty
    ) {
        if (min != null && min < 0) {
            throw new TableFilterValidationException(minProperty + " must be non-negative");
        }
        if (max != null && max < 0) {
            throw new TableFilterValidationException(maxProperty + " must be non-negative");
        }
        if (min != null && max != null && min > max) {
            throw new TableFilterValidationException(
                minProperty + " must be less than or equal to " + maxProperty
            );
        }
    }

    static void validateInstantRange(
        Instant from,
        String fromProperty,
        Instant to,
        String toProperty
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new TableFilterValidationException(
                fromProperty + " must be before or equal to " + toProperty
            );
        }
    }

    static void validateUuidFilter(String value, String matchMode) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (TableFilterMatchMode.fromQueryValue(matchMode) == TableFilterMatchMode.EXACT) {
            parseUuid(value);
        }
    }

    static void validateMatchMode(String value, String matchMode) {
        if (value == null || value.isBlank()) {
            return;
        }
        TableFilterMatchMode.fromQueryValue(matchMode);
    }

    static void validateSort(Sort sort, Set<String> allowedProperties) {
        for (Sort.Order order : sort) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new TableFilterValidationException(
                    "Unsupported sort property: " + order.getProperty()
                );
            }
        }
    }

    static Pageable withDefaultSort(Pageable pageable, Sort defaultSort) {
        return pageable.getSort().isSorted()
            ? pageable
            : PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                defaultSort
            );
    }

    static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new TableFilterValidationException("UUID filter must be valid for exact match");
        }
    }
}
