package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import io.github.HenriqueMichelini.craftalism.api.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<Transaction> getAllTransactions() {
        return service.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getTransactionById(@PathVariable Long id) {
        return service.getTransactionById(id);
    }

    @GetMapping("/from/{uuid}")
    public List<Transaction> getTransactionsByFromBalance(@PathVariable UUID uuid) {
        Balance balance = balanceService.getBalance(uuid);
        return service.getTransactionByFromBalance(balance);
    }

    @GetMapping("/to/{uuid}")
    public List<Transaction> getTransactionsByToBalance(@PathVariable UUID uuid) {
        Balance balance = balanceService.getBalance(uuid);
        return service.getTransactionByToBalance(balance);
    }
}
