package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, UUID> {
    @Query(
        value = "SELECT * FROM balances ORDER BY amount DESC LIMIT :limit",
        nativeQuery = true
    )
    List<Balance> findTopByOrderByAmountDesc(@Param("limit") int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM balance b WHERE b.uuid = :uuid")
    Optional<Balance> findForUpdate(@Param("uuid") UUID uuid);
}
