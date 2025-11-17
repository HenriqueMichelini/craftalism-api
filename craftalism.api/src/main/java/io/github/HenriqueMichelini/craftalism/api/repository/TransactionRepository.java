package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromUuid(UUID fromBalance);
    List<Transaction> findByToUuid(UUID toBalance);
}