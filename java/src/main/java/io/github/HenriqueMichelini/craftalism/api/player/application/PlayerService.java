package io.github.HenriqueMichelini.craftalism.api.player.application;

import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerInUseException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNameAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository repository;
    private final BalanceRepository balanceRepository;

    public PlayerService(
        PlayerRepository repository,
        BalanceRepository balanceRepository
    ) {
        this.repository = repository;
        this.balanceRepository = balanceRepository;
    }

    public List<Player> getAllPlayers() {
        return repository.findAll();
    }

    public Player getPlayerByUuid(UUID uuid) {
        return repository
            .findById(uuid)
            .orElseThrow(() -> new PlayerNotFoundException(uuid));
    }

    public Player getPlayerByName(String name) {
        return repository
            .findByName(name.trim())
            .orElseThrow(() -> new PlayerNotFoundException(name));
    }

    @Transactional
    public Player createPlayer(UUID uuid, String name) {
        if (repository.existsById(uuid)) throw new PlayerAlreadyExistsException(
            uuid
        );
        String trimmedName = name.trim();
        repository
            .findByName(trimmedName)
            .ifPresent(existing -> {
                throw new PlayerNameAlreadyExistsException(trimmedName);
            });

        Player player = new Player();
        player.setUuid(uuid);
        player.setName(trimmedName);
        return repository.save(player);
    }

    @Transactional
    public Player updatePlayer(UUID uuid, String name) {
        Player player = getPlayerByUuid(uuid);
        String trimmedName = name.trim();

        repository
            .findByName(trimmedName)
            .filter(existing -> !existing.getUuid().equals(uuid))
            .ifPresent(existing -> {
                throw new PlayerNameAlreadyExistsException(trimmedName);
            });

        player.setName(trimmedName);
        return repository.save(player);
    }

    @Transactional
    public void deletePlayer(UUID uuid) {
        Player player = getPlayerByUuid(uuid);
        if (balanceRepository.existsById(uuid)) throw new PlayerInUseException(
            uuid
        );

        try {
            repository.delete(player);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new PlayerInUseException(uuid);
        }
    }
}
