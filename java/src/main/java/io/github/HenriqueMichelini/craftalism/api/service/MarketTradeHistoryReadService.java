package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TableFilterMatchMode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketTradeHistoryNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
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
        TableFilterValidation.validateSort(pageable.getSort(), SORT_PROPERTIES);
        return repository
            .findAll(
                specification(filter),
                TableFilterValidation.withDefaultSort(pageable, DEFAULT_SORT)
            )
            .map(this::toDto);
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
            .and(TableFilterSpecifications.uuidMatches("playerUuid", filter.playerUuid(), filter.playerUuidMatch()))
            .and(itemIdMatches(filter.itemId(), filter.itemIdMatch()))
            .and(sideEquals(filter.side()))
            .and(TableFilterSpecifications.greaterThanOrEqualTo("totalPrice", filter.minTotalPrice()))
            .and(TableFilterSpecifications.lessThanOrEqualTo("totalPrice", filter.maxTotalPrice()))
            .and(TableFilterSpecifications.greaterThanOrEqualTo("executedAt", filter.executedFrom()))
            .and(TableFilterSpecifications.lessThanOrEqualTo("executedAt", filter.executedTo()));
    }

    private void validate(MarketTradeHistoryFilterDTO filter) {
        TableFilterValidation.validateNonNegativeRange(
            filter.minTotalPrice(),
            "minTotalPrice",
            filter.maxTotalPrice(),
            "maxTotalPrice"
        );
        TableFilterValidation.validateInstantRange(
            filter.executedFrom(),
            "executedFrom",
            filter.executedTo(),
            "executedTo"
        );
        TableFilterValidation.validateUuidFilter(
            filter.playerUuid(),
            filter.playerUuidMatch()
        );
        TableFilterValidation.validateMatchMode(filter.itemId(), filter.itemIdMatch());
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
