package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketQuoteRepository extends JpaRepository<MarketQuote, String> {
    @Modifying
    @Query("DELETE FROM market_quotes q WHERE q.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
