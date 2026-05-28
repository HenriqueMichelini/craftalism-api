package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MarketEventPricingService {

    private static final EventPricingContext NEUTRAL =
        new EventPricingContext(10_000L, null, null);

    private final MarketEventLifecycleService lifecycleService;
    private final ThreadLocal<Optional<MarketEventInstance>> activeEventCache =
        new ThreadLocal<>();

    public MarketEventPricingService(
        MarketEventLifecycleService lifecycleService
    ) {
        this.lifecycleService = lifecycleService;
    }

    EventPricingContext contextFor(MarketItem item) {
        return activeEvent()
            .filter(event -> targetsItem(event, item))
            .map(event ->
                new EventPricingContext(
                    event.getEffectBasisPoints(),
                    event.getId(),
                    event.getEffectVersion()
                )
            )
            .orElse(NEUTRAL);
    }

    void clearRequestCache() {
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

    private boolean targetsItem(MarketEventInstance event, MarketItem item) {
        if (event.getScope() == MarketEventScope.MARKET_WIDE) {
            return true;
        }
        if (event.getScope() == MarketEventScope.CATEGORY) {
            return item.getCategoryId().equals(event.getSelectedCategoryId());
        }
        if (
            event.getScope() == MarketEventScope.ITEM ||
            event.getScope() == MarketEventScope.ITEM_SET
        ) {
            return selectedItems(event).anyMatch(item.getItemId()::equals);
        }
        return false;
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

    record EventPricingContext(
        long multiplierBasisPoints,
        Long eventInstanceId,
        Integer effectVersion
    ) {}
}
