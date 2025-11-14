package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import io.github.HenriqueMichelini.craftalism.api.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;
    private final BalanceService balanceService;

    public TransactionController(TransactionService service, BalanceService balanceService) {
        this.service = service;
        this.balanceService = balanceService;
    }

    @GetMapping
    public List<TransactionResponseDTO> getAllTransactions() {
        return service.getAllTransactions()
                .stream()
                .map(service::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public TransactionResponseDTO getTransactionById(@PathVariable Long id) {
        return service.toDto(service.getTransactionById(id));
    }

    @GetMapping("/from/{uuid}")
    public List<TransactionResponseDTO> getTransactionsByFromBalance(@PathVariable UUID uuid) {
        Balance balance = balanceService.getBalance(uuid);
        return service.getTransactionByFromBalance(balance)
                .stream()
                .map(service::toDto)
                .toList();
    }

    @GetMapping("/to/{uuid}")
    public List<TransactionResponseDTO> getTransactionsByToBalance(@PathVariable UUID uuid) {
        Balance balance = balanceService.getBalance(uuid);
        return service.getTransactionByToBalance(balance)
                .stream()
                .map(service::toDto)
                .toList();
    }

    @PostMapping
    public TransactionResponseDTO createTransaction(@RequestBody TransactionRequestDTO dto) {
        return service.processTransaction(dto);
    }
}

