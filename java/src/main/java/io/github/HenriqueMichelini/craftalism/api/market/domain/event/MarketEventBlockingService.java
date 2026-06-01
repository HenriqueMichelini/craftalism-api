package io.github.HenriqueMichelini.craftalism.api.market.domain.event;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarketEventBlockingService {

    private final MarketEventLifecycleService lifecycleService;
    private final ThreadLocal<Optional<MarketEventInstance>> activeEventCache =
        new ThreadLocal<>();

    public MarketEventBlockingService(
        MarketEventLifecycleService lifecycleService
    ) {
        this.lifecycleService = lifecycleService;
    }

    public boolean isEffectivelyBlocked(MarketItem item) {
        return (
            item.isBlocked() ||
            activeEvent()
                .filter(MarketEventInstance::isBlocking)
                .filter(event -> event.getScope() == MarketEventScope.ITEM)
                .filter(event -> selectedItems(event).anyMatch(item.getItemId()::equals))
                .isPresent()
        );
    }

    public void clearRequestCache() {
        activeEventCache.remove();
    }

    private Optional<MarketEventInstance> activeEvent() {
        Optional<MarketEventInstance> cached = activeEventCache.get();
        if (cached != null) {
            return cached;
        }
        Optional<MarketEventInstance> active =
            lifecycleService.effectiveActiveEvent(Instant.now());
        activeEventCache.set(active);
        return active;
    }

    private java.util.stream.Stream<String> selectedItems(
        MarketEventInstance event
    ) {
        return Optional
            .ofNullable(event.getSelectedItemIds())
            .stream()
            .flatMap(value -> Arrays.stream(value.split(",")))
            .map(String::trim)
            .filter(value -> !value.isBlank());
    }
}
