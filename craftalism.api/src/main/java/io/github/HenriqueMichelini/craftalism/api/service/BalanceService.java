package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BalanceService {

    private final BalanceRepository repository;

    public BalanceService(BalanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Balance getBalance(UUID uuid) {
        return repository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Balance not found for UUID: " + uuid));
    }

    @Transactional
    public Balance createBalance(UUID uuid) {
        if (repository.existsById(uuid))
            throw new IllegalArgumentException("Balance already exists for UUID: " + uuid);

        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(0L);
        return repository.save(balance);
    }

    @Transactional
    public Balance updateBalance(UUID uuid, Long newAmount) {
        if (newAmount < 0)
            throw new IllegalArgumentException("Amount must be non-negative.");

        Balance balance = getBalance(uuid);
        balance.setAmount(newAmount);
        return repository.save(balance);
    }
}
