package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketQuoteRepository extends JpaRepository<MarketQuote, String> {
    long countByStatus(MarketQuote.Status status);

    Optional<MarketQuote> findByQuoteToken(String quoteToken);

    @Modifying
    @Query(
        """
        UPDATE market_quotes q
        SET q.status = :targetStatus, q.resolvedAt = :resolvedAt
        WHERE q.quoteToken = :quoteToken AND q.status = :expectedStatus
        """
    )
    int transitionStatus(
        @Param("quoteToken") String quoteToken,
        @Param("expectedStatus") MarketQuote.Status expectedStatus,
        @Param("targetStatus") MarketQuote.Status targetStatus,
        @Param("resolvedAt") Instant resolvedAt
    );

    @Modifying
    @Query(
        """
        UPDATE market_quotes q
        SET q.status = :expiredStatus, q.resolvedAt = :resolvedAt
        WHERE q.status = :activeStatus AND q.expiresAt < :cutoff
        """
    )
    int expireActiveQuotes(
        @Param("cutoff") Instant cutoff,
        @Param("activeStatus") MarketQuote.Status activeStatus,
        @Param("expiredStatus") MarketQuote.Status expiredStatus,
        @Param("resolvedAt") Instant resolvedAt
    );
}
