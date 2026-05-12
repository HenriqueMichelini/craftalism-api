package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TableFilterMatchMode;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TableFilterValidationException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TransactionNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import jakarta.persistence.criteria.Expression;
import java.time.Instant;
import java.util.List;
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
public class TransactionService {

    private static final Sort DEFAULT_SORT = Sort
        .by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final Set<String> SORT_PROPERTIES = Set.of(
        "id",
        "fromPlayerUuid",
        "toPlayerUuid",
        "amount",
        "createdAt"
    );

    private final TransactionRepository repository;
    private final PlayerRepository playerRepository;
    private final TransactionMapper mapper;

    public TransactionService(
        TransactionRepository repository,
        PlayerRepository playerRepository,
        TransactionMapper mapper
    ) {
        this.repository = repository;
        this.playerRepository = playerRepository;
        this.mapper = mapper;
    }

    @Transactional
    public TransactionResponseDTO processTransaction(
        TransactionRequestDTO dto
    ) {
        long amount = dto.amount();
        if (amount <= 0) throw new InvalidAmountException();
        if (!playerRepository.existsById(dto.fromPlayerUuid())) {
            throw new PlayerNotFoundException(dto.fromPlayerUuid());
        }
        if (!playerRepository.existsById(dto.toPlayerUuid())) {
            throw new PlayerNotFoundException(dto.toPlayerUuid());
        }

        Transaction tx = new Transaction(
            dto.fromPlayerUuid(),
            dto.toPlayerUuid(),
            amount
        );
        Transaction saved = repository.save(tx);

        return mapper.toDto(saved);
    }

    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    public Page<TransactionResponseDTO> findTransactions(
        TransactionFilterDTO filter,
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

        return repository
            .findAll(specification(filter), effectivePageable)
            .map(mapper::toDto);
    }

    public Transaction getTransactionById(Long id) {
        return repository
            .findById(id)
            .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    public List<Transaction> getTransactionsByFromUuid(UUID fromUuid) {
        return repository.findByFromPlayerUuid(fromUuid);
    }

    public List<Transaction> getTransactionsByToUuid(UUID toUuid) {
        return repository.findByToPlayerUuid(toUuid);
    }

    private void validate(TransactionFilterDTO filter) {
        validateAmountRange(filter.minAmount(), filter.maxAmount());
        validateInstantRange(filter.createdFrom(), filter.createdTo());
        validateUuidFilter(filter.fromPlayerUuid(), filter.fromPlayerUuidMatch());
        validateUuidFilter(filter.toPlayerUuid(), filter.toPlayerUuidMatch());
    }

    private void validateAmountRange(Long minAmount, Long maxAmount) {
        if (minAmount != null && minAmount < 0) {
            throw new TableFilterValidationException("minAmount must be non-negative");
        }
        if (maxAmount != null && maxAmount < 0) {
            throw new TableFilterValidationException("maxAmount must be non-negative");
        }
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            throw new TableFilterValidationException(
                "minAmount must be less than or equal to maxAmount"
            );
        }
    }

    private void validateInstantRange(Instant createdFrom, Instant createdTo) {
        if (
            createdFrom != null &&
            createdTo != null &&
            createdFrom.isAfter(createdTo)
        ) {
            throw new TableFilterValidationException(
                "createdFrom must be before or equal to createdTo"
            );
        }
    }

    private void validateUuidFilter(String value, String matchMode) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (TableFilterMatchMode.fromQueryValue(matchMode) == TableFilterMatchMode.EXACT) {
            parseUuid(value);
        }
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

    private Specification<Transaction> specification(TransactionFilterDTO filter) {
        return ((Specification<Transaction>) (root, query, builder) -> builder.conjunction())
            .and(uuidMatches("fromPlayerUuid", filter.fromPlayerUuid(), filter.fromPlayerUuidMatch()))
            .and(uuidMatches("toPlayerUuid", filter.toPlayerUuid(), filter.toPlayerUuidMatch()))
            .and(amountGreaterThanOrEqualTo(filter.minAmount()))
            .and(amountLessThanOrEqualTo(filter.maxAmount()))
            .and(createdAtGreaterThanOrEqualTo(filter.createdFrom()))
            .and(createdAtLessThanOrEqualTo(filter.createdTo()));
    }

    private Specification<Transaction> uuidMatches(
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
                return builder.equal(root.get(property), parseUuid(value));
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

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new TableFilterValidationException("UUID filter must be valid for exact match");
        }
    }

    private Specification<Transaction> amountGreaterThanOrEqualTo(Long minAmount) {
        return (root, query, builder) ->
            minAmount == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    private Specification<Transaction> amountLessThanOrEqualTo(Long maxAmount) {
        return (root, query, builder) ->
            maxAmount == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }

    private Specification<Transaction> createdAtGreaterThanOrEqualTo(
        Instant createdFrom
    ) {
        return (root, query, builder) ->
            createdFrom == null
                ? builder.conjunction()
                : builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<Transaction> createdAtLessThanOrEqualTo(Instant createdTo) {
        return (root, query, builder) ->
            createdTo == null
                ? builder.conjunction()
                : builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }
}
