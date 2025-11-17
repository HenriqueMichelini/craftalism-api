package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
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

        Balance from = balanceService.getBalance(dto.fromUuid());
        Balance to = balanceService.getBalance(dto.toUuid());

        if (from.getAmount() < amount) {
            throw new IllegalArgumentException("Insufficient balance.");
        }

        balanceService.updateBalance(dto.fromUuid(), from.getAmount() - amount);
        balanceService.updateBalance(dto.toUuid(), to.getAmount() + amount);

        Transaction tx = new Transaction(dto.fromUuid(), dto.toUuid(), amount);
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
    public List<Transaction> getTransactionByFromBalance(UUID fromBalance) {
        return repository.findByFromUuid(fromBalance);
    }

    @Transactional
    public List<Transaction> getTransactionByToBalance(UUID toBalance) {
        return repository.findByToUuid(toBalance);
    }
}

