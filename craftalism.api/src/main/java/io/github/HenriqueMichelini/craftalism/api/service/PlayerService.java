package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlayerService {

    private final PlayerRepository repository;

    public PlayerService(PlayerRepository repository) {
        this.repository = repository;
    }

    public Player getPlayerByUuid(UUID uuid) {
        return repository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Player not found for UUID: " + uuid));
    }

    public Player getPlayerByName(String name) {
        return repository.findByName(name.trim())
                .orElseThrow(() -> new IllegalArgumentException("Player not found for name: " + name));
    }

    @Transactional
    public Player createPlayer(UUID uuid, String name) {
        if (repository.existsById(uuid))
            throw new IllegalArgumentException("Player already exists for UUID: " + uuid);

        Player player = new Player();
        player.setUuid(uuid);
        player.setName(name.trim());
        return repository.save(player);
    }
}
