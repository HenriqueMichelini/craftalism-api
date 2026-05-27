package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketItemRepository extends JpaRepository<MarketItem, String> {
    @Query(
        "SELECT m FROM market_items m JOIN FETCH m.category c ORDER BY c.displayOrder ASC, c.categoryId ASC, m.displayName ASC"
    )
    List<MarketItem> findAllForMarketRead();

    Optional<MarketItem> findByItemId(String itemId);

    boolean existsByCategoryId(String categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM market_items m WHERE m.itemId = :itemId")
    Optional<MarketItem> findForUpdate(@Param("itemId") String itemId);
}
