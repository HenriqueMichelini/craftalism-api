package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegmentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketSegmentRepository extends JpaRepository<MarketSegment, MarketSegmentId> {
    List<MarketSegment> findByItemIdOrderBySegmentIndexAsc(String itemId);
}
