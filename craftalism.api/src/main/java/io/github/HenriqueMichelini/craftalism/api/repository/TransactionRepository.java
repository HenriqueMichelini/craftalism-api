package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromBalance(Balance fromBalance);
    List<Transaction> findByToBalance(Balance toBalance);
}