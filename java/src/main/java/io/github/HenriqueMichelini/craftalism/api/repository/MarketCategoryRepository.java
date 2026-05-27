package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketCategoryRepository
    extends JpaRepository<MarketCategory, String> {
    @Query(
        "SELECT c FROM market_categories c ORDER BY c.displayOrder ASC, c.categoryId ASC"
    )
    List<MarketCategory> findAllForMarketRead();

    Optional<MarketCategory> findByCategoryId(String categoryId);
}
