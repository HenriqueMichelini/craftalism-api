package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSchedulerLock;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketEventSchedulerLockRepository
    extends JpaRepository<MarketEventSchedulerLock, String> {
    @Modifying
    @Query(
        """
        UPDATE market_event_scheduler_locks lock
        SET lock.owner = :owner,
            lock.leaseUntil = :leaseUntil,
            lock.updatedAt = :now
        WHERE lock.lockName = :lockName
          AND lock.leaseUntil <= :now
        """
    )
    int acquireExpiredLease(
        @Param("lockName") String lockName,
        @Param("owner") String owner,
        @Param("now") Instant now,
        @Param("leaseUntil") Instant leaseUntil
    );
}
