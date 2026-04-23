package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketItemRepository extends JpaRepository<MarketItem, String> {
    @EntityGraph(attributePaths = "segments")
    List<MarketItem> findAllByOrderByCategoryIdAscDisplayNameAsc();

    @EntityGraph(attributePaths = "segments")
    Optional<MarketItem> findByItemId(String itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT m FROM market_items m LEFT JOIN FETCH m.segments WHERE m.itemId = :itemId")
    Optional<MarketItem> findForUpdate(@Param("itemId") String itemId);
}
