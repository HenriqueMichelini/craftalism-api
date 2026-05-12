package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TableFilterMatchMode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketTradeHistoryNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TableFilterValidationException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import jakarta.persistence.criteria.Expression;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MarketTradeHistoryReadService {

    private static final Sort DEFAULT_SORT = Sort
        .by(Sort.Order.desc("executedAt"), Sort.Order.desc("id"));
    private static final Set<String> SORT_PROPERTIES = Set.of(
        "id",
        "playerUuid",
        "itemId",
        "side",
        "quantity",
        "unitPrice",
        "totalPrice",
        "executedAt"
    );

    private final MarketTradeHistoryRepository repository;

    public MarketTradeHistoryReadService(MarketTradeHistoryRepository repository) {
        this.repository = repository;
    }

    public Page<MarketTradeHistoryDTO> findTrades(
        MarketTradeHistoryFilterDTO filter,
        Pageable pageable
    ) {
        validate(filter);
        validateSort(pageable.getSort());

        Pageable effectivePageable = pageable.getSort().isSorted()
            ? pageable
            : PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                DEFAULT_SORT
            );
        return repository.findAll(specification(filter), effectivePageable).map(this::toDto);
    }

    public MarketTradeHistoryDTO getTrade(Long id) {
        return repository
            .findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new MarketTradeHistoryNotFoundException(id));
    }

    private Specification<MarketTradeHistory> specification(
        MarketTradeHistoryFilterDTO filter
    ) {
        return ((Specification<MarketTradeHistory>) (root, query, builder) -> builder.conjunction())
            .and(playerUuidMatches(filter.playerUuid(), filter.playerUuidMatch()))
            .and(itemIdMatches(filter.itemId(), filter.itemIdMatch()))
            .and(sideEquals(filter.side()))
            .and(totalPriceGreaterThanOrEqualTo(filter.minTotalPrice()))
            .and(totalPriceLessThanOrEqualTo(filter.maxTotalPrice()))
            .and(executedAtGreaterThanOrEqualTo(filter.executedFrom()))
            .and(executedAtLessThanOrEqualTo(filter.executedTo()));
    }

    private void validate(MarketTradeHistoryFilterDTO filter) {
        validateTotalPriceRange(filter.minTotalPrice(), filter.maxTotalPrice());
        validateInstantRange(filter.executedFrom(), filter.executedTo());
        validatePlayerUuidFilter(filter.playerUuid(), filter.playerUuidMatch());
        validateMatchMode(filter.itemId(), filter.itemIdMatch());
    }

    private void validateTotalPriceRange(Long minTotalPrice, Long maxTotalPrice) {
        if (minTotalPrice != null && minTotalPrice < 0) {
            throw new TableFilterValidationException(
                "minTotalPrice must be non-negative"
            );
        }
        if (maxTotalPrice != null && maxTotalPrice < 0) {
            throw new TableFilterValidationException(
                "maxTotalPrice must be non-negative"
            );
        }
        if (
            minTotalPrice != null &&
            maxTotalPrice != null &&
            minTotalPrice > maxTotalPrice
        ) {
            throw new TableFilterValidationException(
                "minTotalPrice must be less than or equal to maxTotalPrice"
            );
        }
    }

    private void validateInstantRange(Instant executedFrom, Instant executedTo) {
        if (
            executedFrom != null &&
            executedTo != null &&
            executedFrom.isAfter(executedTo)
        ) {
            throw new TableFilterValidationException(
                "executedFrom must be before or equal to executedTo"
            );
        }
    }

    private void validatePlayerUuidFilter(String value, String matchMode) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (TableFilterMatchMode.fromQueryValue(matchMode) == TableFilterMatchMode.EXACT) {
            parseUuid(value);
        }
    }

    private void validateMatchMode(String value, String matchMode) {
        if (value == null || value.isBlank()) {
            return;
        }
        TableFilterMatchMode.fromQueryValue(matchMode);
    }

    private void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORT_PROPERTIES.contains(order.getProperty())) {
                throw new TableFilterValidationException(
                    "Unsupported sort property: " + order.getProperty()
                );
            }
        }
    }

    private Specification<MarketTradeHistory> playerUuidMatches(
        String playerUuid,
        String matchMode
    ) {
        return (root, query, builder) -> {
            if (playerUuid == null || playerUuid.isBlank()) {
                return builder.conjunction();
            }

            TableFilterMatchMode mode = TableFilterMatchMode.fromQueryValue(matchMode);
            if (mode == TableFilterMatchMode.EXACT) {
                return builder.equal(root.get("playerUuid"), parseUuid(playerUuid));
            }

            Expression<String> uuidText = builder.lower(
                ((JpaExpression<?>) root.get("playerUuid")).cast(String.class)
            );
            return builder.like(
                uuidText,
                "%" + playerUuid.trim().toLowerCase(Locale.ROOT) + "%"
            );
        };
    }

    private Specification<MarketTradeHistory> itemIdMatches(
        String itemId,
        String matchMode
    ) {
        return (root, query, builder) -> {
            if (itemId == null || itemId.isBlank()) {
                return builder.conjunction();
            }

            TableFilterMatchMode mode = TableFilterMatchMode.fromQueryValue(matchMode);
            if (mode == TableFilterMatchMode.EXACT) {
                return builder.equal(root.get("itemId"), itemId.trim());
            }

            return builder.like(
                builder.lower(root.get("itemId")),
                "%" + itemId.trim().toLowerCase(Locale.ROOT) + "%"
            );
        };
    }

    private Specification<MarketTradeHistory> sideEquals(MarketSide side) {
        return (root, query, builder) ->
            side == null
                ? builder.conjunction()
                : builder.equal(root.get("side"), side);
    }

    private Specification<MarketTradeHistory> totalPriceGreaterThanOrEqualTo(
        Long minTotalPrice
    ) {
        return (root, query, builder) ->
            minTotalPrice == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("totalPrice"), minTotalPrice);
    }

    private Specification<MarketTradeHistory> totalPriceLessThanOrEqualTo(
        Long maxTotalPrice
    ) {
        return (root, query, builder) ->
            maxTotalPrice == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("totalPrice"), maxTotalPrice);
    }

    private Specification<MarketTradeHistory> executedAtGreaterThanOrEqualTo(
        Instant executedFrom
    ) {
        return (root, query, builder) ->
            executedFrom == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("executedAt"), executedFrom);
    }

    private Specification<MarketTradeHistory> executedAtLessThanOrEqualTo(
        Instant executedTo
    ) {
        return (root, query, builder) ->
            executedTo == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("executedAt"), executedTo);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new TableFilterValidationException("UUID filter must be valid for exact match");
        }
    }

    private MarketTradeHistoryDTO toDto(MarketTradeHistory history) {
        return new MarketTradeHistoryDTO(
            history.getId(),
            history.getPlayerUuid(),
            history.getItemId(),
            history.getSide(),
            history.getQuantity(),
            Long.toString(history.getUnitPrice()),
            Long.toString(history.getTotalPrice()),
            history.getCurrency(),
            history.getSnapshotVersion(),
            history.getExecutedAt()
        );
    }
}
