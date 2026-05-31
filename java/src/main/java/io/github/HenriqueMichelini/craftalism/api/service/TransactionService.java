package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TransactionNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
        TableFilterValidation.validateSort(pageable.getSort(), SORT_PROPERTIES);

        return repository
            .findAll(
                specification(filter),
                TableFilterValidation.withDefaultSort(pageable, DEFAULT_SORT)
            )
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
        TableFilterValidation.validateNonNegativeRange(
            filter.minAmount(),
            "minAmount",
            filter.maxAmount(),
            "maxAmount"
        );
        TableFilterValidation.validateInstantRange(
            filter.createdFrom(),
            "createdFrom",
            filter.createdTo(),
            "createdTo"
        );
        TableFilterValidation.validateUuidFilter(
            filter.fromPlayerUuid(),
            filter.fromPlayerUuidMatch()
        );
        TableFilterValidation.validateUuidFilter(
            filter.toPlayerUuid(),
            filter.toPlayerUuidMatch()
        );
    }

    private Specification<Transaction> specification(TransactionFilterDTO filter) {
        return ((Specification<Transaction>) (root, query, builder) -> builder.conjunction())
            .and(TableFilterSpecifications.uuidMatches("fromPlayerUuid", filter.fromPlayerUuid(), filter.fromPlayerUuidMatch()))
            .and(TableFilterSpecifications.uuidMatches("toPlayerUuid", filter.toPlayerUuid(), filter.toPlayerUuidMatch()))
            .and(TableFilterSpecifications.greaterThanOrEqualTo("amount", filter.minAmount()))
            .and(TableFilterSpecifications.lessThanOrEqualTo("amount", filter.maxAmount()))
            .and(TableFilterSpecifications.greaterThanOrEqualTo("createdAt", filter.createdFrom()))
            .and(TableFilterSpecifications.lessThanOrEqualTo("createdAt", filter.createdTo()));
    }
}
