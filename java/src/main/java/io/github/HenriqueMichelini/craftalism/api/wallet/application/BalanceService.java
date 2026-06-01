package io.github.HenriqueMichelini.craftalism.api.wallet.application;

import io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceArithmeticOverflowException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InsufficientFundsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BalanceService {

    private final BalanceRepository repository;
    private final PlayerRepository playerRepository;

    public BalanceService(
        BalanceRepository repository,
        PlayerRepository playerRepository
    ) {
        this.repository = repository;
        this.playerRepository = playerRepository;
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
        if (initialAmount < 0) throw new InvalidAmountException();
        if (!playerRepository.existsById(uuid)) throw new PlayerNotFoundException(
            uuid
        );
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
        balance.setAmount(addBalanceAmounts(balance.getAmount(), amount));
        return repository.save(balance);
    }

    public List<Balance> getTopBalances(int limit) {
        if (limit <= 0) limit = 10;
        if (limit > 20) limit = 20;
        return repository.findTopBalances(limit);
    }

    @Transactional
    public Balance setBalance(UUID uuid, long newAmount) {
        if (newAmount < 0) throw new InvalidAmountException();
        Balance balance = getBalance(uuid);
        balance.setAmount(newAmount);
        return repository.save(balance);
    }

    private long addBalanceAmounts(long currentAmount, long amount) {
        try {
            return Math.addExact(currentAmount, amount);
        } catch (ArithmeticException ex) {
            throw new BalanceArithmeticOverflowException();
        }
    }

    @Transactional
    public void deleteBalance(UUID uuid) {
        Balance balance = getBalance(uuid);
        repository.delete(balance);
    }
}
