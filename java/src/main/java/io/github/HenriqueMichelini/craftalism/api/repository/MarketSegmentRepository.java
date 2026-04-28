package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegmentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketSegmentRepository extends JpaRepository<MarketSegment, MarketSegmentId> {
    @Query("SELECT s FROM market_segments s WHERE s.id.itemId = :itemId ORDER BY s.id.segmentIndex ASC")
    List<MarketSegment> findByItemIdOrderBySegmentIndexAsc(@Param("itemId") String itemId);
}
