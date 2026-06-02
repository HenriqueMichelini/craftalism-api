package io.github.HenriqueMichelini.craftalism.api.market.application.admin;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketDriftResetResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlanner;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MarketDriftAdminService {

    private static final long NEUTRAL_MULTIPLIER_BASIS_POINTS = 10_000L;

    private final MarketItemRepository marketItemRepository;
    private final MarketTradePlanner tradePlanner;

    public MarketDriftAdminService(
        MarketItemRepository marketItemRepository,
        MarketTradePlanner tradePlanner
    ) {
        this.marketItemRepository = marketItemRepository;
        this.tradePlanner = tradePlanner;
    }

    @Transactional
    public MarketDriftResetResponseDTO resetAllDrift() {
        tradePlanner.clearPricingCache();
        try {
            Instant now = Instant.now();
            List<MarketItem> items = marketItemRepository.findAll();
            for (MarketItem item : items) {
                item.setDriftMultiplierBasisPoints(
                    NEUTRAL_MULTIPLIER_BASIS_POINTS
                );
                item.setDriftRevision(item.getDriftRevision() + 1L);
                item.setDriftEvaluatedAt(now);
                tradePlanner.recomputeDerivedProjections(item);
            }
            marketItemRepository.saveAll(items);
            return new MarketDriftResetResponseDTO(
                items.size(),
                NEUTRAL_MULTIPLIER_BASIS_POINTS,
                now
            );
        } finally {
            tradePlanner.clearPricingCache();
        }
    }
}
