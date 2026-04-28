package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TransactionNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransactionService {

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
}
