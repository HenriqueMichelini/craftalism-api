package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/balances")
public class BalanceController {
    private final BalanceService service;

    public BalanceController(BalanceService service) {
        this.service = service;
    }

    @GetMapping("/{uuid}")
    public Balance getBalanceByUuid(@PathVariable UUID uuid) {
        return service.getBalance(uuid);
    }

    @PostMapping()
    public Balance createBalance(@RequestParam UUID uuid) {
        return service.createBalance(uuid);
    }

    @PutMapping("/{uuid}")
    public Balance updateBalance(@PathVariable UUID uuid, @RequestParam Long amount) {
        service.updateBalance(uuid, amount);
        return service.getBalance(uuid);
    }
}