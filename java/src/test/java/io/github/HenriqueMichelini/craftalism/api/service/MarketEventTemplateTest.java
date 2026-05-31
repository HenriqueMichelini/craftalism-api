package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketEventTemplateTest {

    @Mock
    private MarketEventTemplateRepository templateRepository;

    @Test
    void seedInitialTemplatesIfEmptySeedsAuthoredTemplates() {
        when(templateRepository.count()).thenReturn(0L);

        new MarketEventTemplateService(
            templateRepository
        ).seedInitialTemplatesIfEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MarketEventTemplate>> templatesCaptor =
            ArgumentCaptor.forClass(Iterable.class);
        verify(templateRepository).saveAll(templatesCaptor.capture());

        List<String> templateIds = new ArrayList<>();
        templatesCaptor
            .getValue()
            .forEach(template -> templateIds.add(template.getTemplateId()));
        assertEquals(
            List.of(
                "farming_bumper_crop",
                "mining_tool_shortage",
                "rare_customs_hold",
                "extra_rare_market_alarm"
            ),
            templateIds
        );
    }

    @Test
    void seedInitialTemplatesIfEmptyLeavesPopulatedRepositoryUnchanged() {
        when(templateRepository.count()).thenReturn(1L);

        new MarketEventTemplateService(
            templateRepository
        ).seedInitialTemplatesIfEmpty();

        verify(templateRepository, never()).saveAll(any());
    }

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
