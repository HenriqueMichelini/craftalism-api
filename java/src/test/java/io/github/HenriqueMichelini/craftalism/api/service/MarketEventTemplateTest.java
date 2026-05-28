package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketEventTemplateTest {

    @Test
    void initialTemplatesIncludeAutomaticMediumAndManualOnlyRareTemplates() {
        MarketEventTemplateService service = new MarketEventTemplateService(
            null
        );

        List<MarketEventTemplate> templates = service.initialTemplates(
            Instant.parse("2026-01-01T00:00:00Z")
        );

        assertTrue(
            templates
                .stream()
                .anyMatch(template ->
                    template.getRarity() == MarketEventRarity.MEDIUM &&
                    template.isAutomaticEnabled() &&
                    template.getAutomaticWeight() > 0
                )
        );
        assertTrue(
            templates
                .stream()
                .anyMatch(template ->
                    template.getRarity() == MarketEventRarity.RARE &&
                    template.isBlockingAllowed() &&
                    !template.isAutomaticEnabled()
                )
        );
        assertFalse(
            templates
                .stream()
                .filter(template ->
                    template.getRarity() == MarketEventRarity.EXTRA_RARE
                )
                .anyMatch(MarketEventTemplate::isAutomaticEnabled)
        );
        assertFalse(
            templates.stream().anyMatch(this::isNeutralNoEffectTemplate)
        );
    }

    private boolean isNeutralNoEffectTemplate(MarketEventTemplate template) {
        return (
            !template.isBlockingAllowed() &&
            template.getMinEffectBasisPoints() == 10_000 &&
            template.getMaxEffectBasisPoints() == 10_000
        );
    }
}
