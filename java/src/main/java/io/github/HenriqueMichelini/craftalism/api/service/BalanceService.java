package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InsufficientFundsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidTransferException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BalanceService {

    private final BalanceRepository repository;

    public BalanceService(BalanceRepository repository) {
        this.repository = repository;
    }

    public List<Balance> getAllBalances() {
        return repository.findAll();
    }

    public Balance getBalance(UUID uuid) {
        return repository
            .findById(uuid)
            .orElseThrow(() -> new BalanceNotFoundException(uuid));
    }

    @Transactional
    public Balance createBalance(UUID uuid, long initialAmount) {
        if (
            repository.existsById(uuid)
        ) throw new BalanceAlreadyExistsException(uuid);
        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(initialAmount);
        return repository.save(balance);
    }

    @Transactional
    public Balance withdraw(UUID uuid, long amount) {
        if (amount <= 0) throw new InvalidAmountException();
        Balance balance = repository
            .findForUpdate(uuid)
            .orElseThrow(() -> new BalanceNotFoundException(uuid));
        if (balance.getAmount() < amount) throw new InsufficientFundsException(
            uuid,
            amount
        );
        balance.setAmount(balance.getAmount() - amount);
        return repository.save(balance);
    }

    @Transactional
    public Balance deposit(UUID uuid, long amount) {
        if (amount <= 0) throw new InvalidAmountException();
        Balance balance = repository
            .findForUpdate(uuid)
            .orElseThrow(() -> new BalanceNotFoundException(uuid));
        balance.setAmount(balance.getAmount() + amount);
        return repository.save(balance);
    }

    @Transactional
    public void transfer(UUID from, UUID to, long amount) {
        if (from.equals(to)) throw new InvalidTransferException();
        if (amount <= 0) throw new InvalidAmountException();
        UUID first = (from.compareTo(to) < 0) ? from : to;
        UUID second = (first.equals(from)) ? to : from;
        Balance firstBalance = repository
            .findForUpdate(first)
            .orElseThrow(() -> new BalanceNotFoundException(first));
        Balance secondBalance = repository
            .findForUpdate(second)
            .orElseThrow(() -> new BalanceNotFoundException(second));
        Balance fromBalance = from.equals(first) ? firstBalance : secondBalance;
        Balance toBalance = to.equals(first) ? firstBalance : secondBalance;
        if (
            fromBalance.getAmount() < amount
        ) throw new InsufficientFundsException(from, amount);
        fromBalance.setAmount(fromBalance.getAmount() - amount);
        toBalance.setAmount(toBalance.getAmount() + amount);
        repository.save(fromBalance);
        repository.save(toBalance);
    }

    public List<Balance> getTopBalances(int limit) {
        if (limit <= 0) limit = 10;
        if (limit > 20) limit = 20;
        return repository.findTopBalances(limit);
    }

    @Transactional
    public Balance setBalance(UUID uuid, long newAmount) {
        Balance balance = getBalance(uuid);
        balance.setAmount(newAmount);
        return repository.save(balance);
    }
}
