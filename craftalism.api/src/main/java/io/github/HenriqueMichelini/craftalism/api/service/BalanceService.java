package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InsufficientFundsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
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
                .orElseThrow(() -> new BalanceNotFoundException(uuid));
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
    public Balance withdraw(UUID uuid, long amount) {
        if (amount <= 0)
            throw new InvalidAmountException();

        Balance balance = repository.findForUpdate(uuid);
        if (balance.getAmount() < amount)
            throw new InsufficientFundsException(uuid, amount);

        balance.setAmount(balance.getAmount() - amount);
        return repository.save(balance);
    }

    @Transactional
    public Balance deposit(UUID uuid, long amount) {
        if (amount <= 0)
            throw new InvalidAmountException();

        Balance balance = repository.findForUpdate(uuid);
        balance.setAmount(balance.getAmount() + amount);
        return repository.save(balance);
    }

    @Transactional
    public void transfer(UUID from, UUID to, long amount) {
        if (from.equals(to)) throw new IllegalArgumentException("From and To must be different.");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0.");

        UUID first = (from.compareTo(to) < 0) ? from : to;
        UUID second = (first.equals(from) ? to : from);

        Balance firstBalance = repository.findForUpdate(first);
        Balance secondBalance = repository.findForUpdate(second);

        Balance fromBalance = from.equals(first) ? firstBalance : secondBalance;
        Balance toBalance   = to.equals(first)   ? firstBalance : secondBalance;

        if (fromBalance.getAmount() < amount) {
            throw new InsufficientFundsException(from, amount);
        }

        fromBalance.setAmount(fromBalance.getAmount() - amount);
        toBalance.setAmount(toBalance.getAmount() + amount);

        repository.save(fromBalance);
        repository.save(toBalance);
    }

    @Transactional
    public List<Balance> getTopBalances(int limit) {
        if (limit <= 0) limit = 10;
        if (limit > 20) limit = 20;

        return repository.findTopByOrderByAmountDesc(limit);
    }

    @Transactional
    public Balance setBalance(UUID uuid, long newAmount) {
        if (newAmount < 0) throw new IllegalArgumentException("Amount must be non-negative.");
        Balance b = getBalance(uuid);
        b.setAmount(newAmount);
        return repository.save(b);
    }
}
