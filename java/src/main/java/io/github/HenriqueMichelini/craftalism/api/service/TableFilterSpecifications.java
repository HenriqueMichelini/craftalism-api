package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TableFilterMatchMode;
import jakarta.persistence.criteria.Expression;
import java.util.Locale;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;

final class TableFilterSpecifications {

    private TableFilterSpecifications() {}

    static <T> Specification<T> uuidMatches(
        String property,
        String value,
        String matchMode
    ) {
        return (root, query, builder) -> {
            if (value == null || value.isBlank()) {
                return builder.conjunction();
            }

            TableFilterMatchMode mode = TableFilterMatchMode.fromQueryValue(matchMode);
            if (mode == TableFilterMatchMode.EXACT) {
                return builder.equal(root.get(property), TableFilterValidation.parseUuid(value));
            }

            Expression<String> uuidText = builder.lower(
                ((JpaExpression<?>) root.get(property)).cast(String.class)
            );
            return builder.like(
                uuidText,
                "%" + value.trim().toLowerCase(Locale.ROOT) + "%"
            );
        };
    }

    static <T, V extends Comparable<? super V>> Specification<T> greaterThanOrEqualTo(
        String property,
        V value
    ) {
        return (root, query, builder) ->
            value == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get(property), value);
    }

    static <T, V extends Comparable<? super V>> Specification<T> lessThanOrEqualTo(
        String property,
        V value
    ) {
        return (root, query, builder) ->
            value == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get(property), value);
    }
}
