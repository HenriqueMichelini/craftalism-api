package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketTradeHistoryRepository
    extends JpaRepository<MarketTradeHistory, Long>, JpaSpecificationExecutor<MarketTradeHistory> {}
