package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final BalanceService balanceService;
    private final TransactionMapper mapper;

    public TransactionService(TransactionRepository repository,
                              BalanceService balanceService,
                              TransactionMapper mapper) {
        this.repository = repository;
        this.balanceService = balanceService;
        this.mapper = mapper;
    }

    @Transactional
    public TransactionResponseDTO processTransaction(TransactionRequestDTO dto) {
        long amount = dto.amount();
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }

        balanceService.transfer(dto.fromPlayerUuid(), dto.toPlayerUuid(), amount);

        Transaction tx = new Transaction(dto.fromPlayerUuid(), dto.toPlayerUuid(), amount);
        Transaction saved = repository.save(tx);

        return mapper.toDto(saved);
    }

    @Transactional
    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    @Transactional
    public Transaction getTransactionById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for id: " + id));
    }

    @Transactional
    public List<Transaction> getTransactionsByFromUuid(UUID fromUuid) {
        return repository.findByFromPlayerUuid(fromUuid);
    }

    @Transactional
    public List<Transaction> getTransactionsByToUuid(UUID toUuid) {
        return repository.findByToPlayerUuid(toUuid);
    }
}

