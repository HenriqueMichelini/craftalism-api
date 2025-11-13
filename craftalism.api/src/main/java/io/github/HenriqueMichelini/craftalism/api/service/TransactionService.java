package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Balance from = balanceService.getBalance(dto.fromUuid());
        Balance to = balanceService.getBalance(dto.toUuid());

        balanceService.updateBalance(from.getUuid(), dto.amount());
        balanceService.updateBalance(to.getUuid(), dto.amount());

        Transaction transaction = new Transaction(from, to, dto.amount());
        Transaction saved = repository.save(transaction);

        return mapper.toDto(saved);
    }
}
