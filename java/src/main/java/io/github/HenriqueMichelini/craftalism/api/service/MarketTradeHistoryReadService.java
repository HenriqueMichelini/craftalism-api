package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketTradeHistoryNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import java.time.Instant;
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

    private final MarketTradeHistoryRepository repository;

    public MarketTradeHistoryReadService(MarketTradeHistoryRepository repository) {
        this.repository = repository;
    }

    public Page<MarketTradeHistoryDTO> findTrades(
        MarketTradeHistoryFilterDTO filter,
        Pageable pageable
    ) {
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
            .and(playerUuidEquals(filter.playerUuid()))
            .and(itemIdEquals(filter.itemId()))
            .and(sideEquals(filter.side()))
            .and(executedAtGreaterThanOrEqualTo(filter.executedFrom()))
            .and(executedAtLessThanOrEqualTo(filter.executedTo()));
    }

    private Specification<MarketTradeHistory> playerUuidEquals(
        java.util.UUID playerUuid
    ) {
        return (root, query, builder) ->
            playerUuid == null
                ? builder.conjunction()
                : builder.equal(root.get("playerUuid"), playerUuid);
    }

    private Specification<MarketTradeHistory> itemIdEquals(String itemId) {
        return (root, query, builder) ->
            itemId == null || itemId.isBlank()
                ? builder.conjunction()
                : builder.equal(root.get("itemId"), itemId);
    }

    private Specification<MarketTradeHistory> sideEquals(MarketSide side) {
        return (root, query, builder) ->
            side == null
                ? builder.conjunction()
                : builder.equal(root.get("side"), side);
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
