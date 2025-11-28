package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    @Query(value = "SELECT * FROM balances ORDER BY amount DESC LIMIT :limit",
            nativeQuery = true)
    List<Balance> findTopByOrderByAmountDesc(@Param("limit") int limit);
}