package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.service.PlayerService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping("/{uuid}")
    public Player getPlayerByUuid(@PathVariable UUID uuid) {
        return service.getPlayerByUuid(uuid);
    }

    @GetMapping("/name/{name}")
    public Player getPlayerByName(@PathVariable String name) {
        return service.getPlayerByName(name);
    }

    @PostMapping
    public Player createPlayer(@RequestParam UUID uuid, @RequestParam String name) {
        return service.createPlayer(uuid, name);
    }
}
