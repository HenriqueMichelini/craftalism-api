package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketEventInstanceRepository
    extends JpaRepository<MarketEventInstance, Long> {
    List<MarketEventInstance> findByStatus(MarketEventStatus status);

    List<MarketEventInstance> findByCreatedAtAfter(Instant createdAt);

    @Query(
        """
        SELECT e FROM market_event_instances e
        WHERE e.status = io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus.ACTIVE
          AND e.startedAt <= :now
          AND e.endsAt > :now
        """
    )
    Optional<MarketEventInstance> findEffectiveActive(@Param("now") Instant now);

    @Modifying
    @Query(
        """
        UPDATE market_event_instances e
        SET e.status = io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus.EXPIRED,
            e.endReason = io.github.HenriqueMichelini.craftalism.api.model.MarketEventEndReason.EXPIRED,
            e.activeSlot = NULL,
            e.updatedAt = :now
        WHERE e.status = io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus.ACTIVE
          AND e.endsAt <= :now
        """
    )
    int expireElapsedActiveEvents(@Param("now") Instant now);
}
