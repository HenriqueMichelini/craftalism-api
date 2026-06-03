package io.github.HenriqueMichelini.craftalism.api.market.domain.event;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public final class MarketEventSelectionPolicy {

    public List<MarketEventTemplate> automaticCandidates(
        List<MarketEventTemplate> templates
    ) {
        return templates
            .stream()
            .filter(MarketEventTemplate::isAutomaticEnabled)
            .filter(template -> template.getAutomaticWeight() > 0)
            .filter(template -> !template.isBlockingAllowed())
            .toList();
    }

    public long longestCooldownSeconds(List<MarketEventTemplate> templates) {
        return templates
            .stream()
            .mapToLong(MarketEventTemplate::getCooldownSeconds)
            .max()
            .orElseThrow();
    }

    public List<MarketEventTemplate> excludeCoolingDown(
        List<MarketEventTemplate> templates,
        Instant now,
        List<MarketEventInstance> recentEvents,
        Function<MarketEventTemplate, String> firstCategoryId,
        Function<MarketEventTemplate, String> firstItemId
    ) {
        return templates
            .stream()
            .filter(template ->
                !isCoolingDown(
                    template,
                    now,
                    recentEvents,
                    firstCategoryId,
                    firstItemId
                )
            )
            .sorted(Comparator.comparing(MarketEventTemplate::getTemplateId))
            .toList();
    }

    public MarketEventTemplate chooseWeightedTemplate(
        List<MarketEventTemplate> templates,
        Random random
    ) {
        long totalWeight = templates
            .stream()
            .mapToLong(MarketEventTemplate::getAutomaticWeight)
            .sum();
        long roll = random.nextLong(totalWeight);
        long cursor = 0L;
        for (MarketEventTemplate template : templates) {
            cursor += template.getAutomaticWeight();
            if (roll < cursor) {
                return template;
            }
        }
        return templates.get(templates.size() - 1);
    }

    private boolean isCoolingDown(
        MarketEventTemplate template,
        Instant now,
        List<MarketEventInstance> recentEvents,
        Function<MarketEventTemplate, String> firstCategoryId,
        Function<MarketEventTemplate, String> firstItemId
    ) {
        Instant cutoff = now.minusSeconds(template.getCooldownSeconds());
        return recentEvents
            .stream()
            .filter(event -> event.getCreatedAt() != null)
            .filter(event -> event.getCreatedAt().isAfter(cutoff))
            .anyMatch(event ->
                event.getTemplateId().equals(template.getTemplateId()) ||
                sameTarget(event, template, firstCategoryId, firstItemId)
            );
    }

    private boolean sameTarget(
        MarketEventInstance event,
        MarketEventTemplate template,
        Function<MarketEventTemplate, String> firstCategoryId,
        Function<MarketEventTemplate, String> firstItemId
    ) {
        return switch (template.getScope()) {
            case CATEGORY -> event.getSelectedCategoryId() != null &&
            event.getSelectedCategoryId().equals(firstCategoryId.apply(template));
            case ITEM, ITEM_SET -> event.getSelectedItemIds() != null &&
            event.getSelectedItemIds().equals(firstItemId.apply(template));
            case MARKET_WIDE -> event.getScope() == MarketEventScope.MARKET_WIDE;
        };
    }
}
