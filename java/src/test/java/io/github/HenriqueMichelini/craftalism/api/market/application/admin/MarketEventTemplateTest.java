package io.github.HenriqueMichelini.craftalism.api.market.application.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketEventTemplateValidationException;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.DefaultMarketEventTemplateCatalog;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventTemplateBuilder;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
            templateRepository,
            new ObjectMapper(),
            new DefaultMarketEventTemplateCatalog()
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
            templateRepository,
            new ObjectMapper(),
            new DefaultMarketEventTemplateCatalog()
        ).seedInitialTemplatesIfEmpty();

        verify(templateRepository, never()).saveAll(any());
    }

    @Test
    void updateTemplatePersistsAuthoredFieldsAndPreservesIdentityAndCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        MarketEventTemplate existing = template(
            "crafting_festival",
            createdAt
        );
        when(templateRepository.findById("crafting_festival")).thenReturn(
            Optional.of(existing)
        );
        when(templateRepository.save(existing)).thenReturn(existing);

        new MarketEventTemplateService(
            templateRepository,
            new ObjectMapper(),
            new DefaultMarketEventTemplateCatalog()
        ).updateTemplate("crafting_festival", validUpdateRequest());

        ArgumentCaptor<MarketEventTemplate> templateCaptor =
            ArgumentCaptor.forClass(MarketEventTemplate.class);
        verify(templateRepository).save(templateCaptor.capture());

        MarketEventTemplate updated = templateCaptor.getValue();
        assertEquals("crafting_festival", updated.getTemplateId());
        assertEquals(createdAt, updated.getCreatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(createdAt));
        assertEquals(MarketEventScope.CATEGORY, updated.getScope());
        assertEquals("DOWN", updated.getEffectDirection());
        assertEquals("Quiet Market", updated.getPlayerFacingName());
        assertEquals(
            "{\"categoryIds\":[\"farming\"]}",
            updated.getEligibleTargetMetadata()
        );
    }

    @Test
    void updateTemplateDerivesBlockDirectionFromNeutralBlockingRange() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        MarketEventTemplate existing = template(
            "rare_customs_hold",
            createdAt
        );
        when(templateRepository.findById("rare_customs_hold")).thenReturn(
            Optional.of(existing)
        );
        when(templateRepository.save(existing)).thenReturn(existing);

        new MarketEventTemplateService(
            templateRepository,
            new ObjectMapper(),
            new DefaultMarketEventTemplateCatalog()
        ).updateTemplate("rare_customs_hold", validBlockUpdateRequest());

        ArgumentCaptor<MarketEventTemplate> templateCaptor =
            ArgumentCaptor.forClass(MarketEventTemplate.class);
        verify(templateRepository).save(templateCaptor.capture());

        MarketEventTemplate updated = templateCaptor.getValue();
        assertEquals("BLOCK", updated.getEffectDirection());
        assertTrue(updated.isBlockingAllowed());
        assertEquals(10_000, updated.getMinEffectBasisPoints());
        assertEquals(10_000, updated.getMaxEffectBasisPoints());
    }

    @Test
    void updateUnknownTemplateReturnsValidationProblemWithoutCreatingTemplate() {
        when(templateRepository.findById("missing")).thenReturn(Optional.empty());

        MarketEventTemplateValidationException exception = assertThrows(
            MarketEventTemplateValidationException.class,
            () ->
                new MarketEventTemplateService(
                    templateRepository,
                    new ObjectMapper(),
                    new DefaultMarketEventTemplateCatalog()
                ).updateTemplate("missing", validUpdateRequest())
        );

        assertEquals(
            "Market event template does not exist.",
            exception.getMessage()
        );
        verify(templateRepository, never()).save(any());
    }

    @Test
    void invalidUpdateDoesNotMutateStoredTemplate() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        MarketEventTemplate existing = template(
            "crafting_festival",
            createdAt
        );
        when(templateRepository.findById("crafting_festival")).thenReturn(
            Optional.of(existing)
        );

        MarketEventTemplateValidationException exception = assertThrows(
            MarketEventTemplateValidationException.class,
            () ->
                new MarketEventTemplateService(
                    templateRepository,
                    new ObjectMapper(),
                    new DefaultMarketEventTemplateCatalog()
                ).updateTemplate(
                    "crafting_festival",
                    invalidDurationUpdateRequest()
                )
        );

        assertEquals(
            "Maximum duration seconds must be greater than or equal to minimum duration seconds.",
            exception.getMessage()
        );
        assertEquals("Crafting Festival", existing.getPlayerFacingName());
        assertEquals(createdAt, existing.getUpdatedAt());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void mixedEffectRangeReturnsValidationProblemWithoutMutatingTemplate() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        MarketEventTemplate existing = template(
            "crafting_festival",
            createdAt
        );
        when(templateRepository.findById("crafting_festival")).thenReturn(
            Optional.of(existing)
        );

        MarketEventTemplateValidationException exception = assertThrows(
            MarketEventTemplateValidationException.class,
            () ->
                new MarketEventTemplateService(
                    templateRepository,
                    new ObjectMapper(),
                    new DefaultMarketEventTemplateCatalog()
                ).updateTemplate(
                    "crafting_festival",
                    mixedEffectRangeUpdateRequest()
                )
        );

        assertEquals(
            "Effect basis point range must be entirely above 10000, entirely below 10000, or exactly 10000 for blocking.",
            exception.getMessage()
        );
        assertEquals("UP", existing.getEffectDirection());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void initialTemplatesUseExplicitAutomaticAndBlockingRules() {
        List<MarketEventTemplate> templates =
            new DefaultMarketEventTemplateCatalog().templates(
            Instant.parse("2026-01-01T00:00:00Z")
        );

        assertTrue(
            templates
                .stream()
                .anyMatch(template ->
                    template.isAutomaticEnabled() &&
                    template.getAutomaticWeight() > 0 &&
                    !template.isBlockingAllowed()
                )
        );
        assertTrue(
            templates
                .stream()
                .anyMatch(template ->
                    template.isBlockingAllowed() &&
                    !template.isAutomaticEnabled() &&
                    template.getScope() == MarketEventScope.ITEM &&
                    template.getMinEffectBasisPoints() == 10_000 &&
                    template.getMaxEffectBasisPoints() == 10_000
                )
        );
        assertFalse(
            templates.stream().anyMatch(template ->
                template.isAutomaticEnabled() && template.isBlockingAllowed()
            )
        );
        assertFalse(
            templates.stream().anyMatch(this::isNeutralNoEffectTemplate)
        );
    }

    @Test
    void defaultCatalogPreservesEveryAuthoredTemplateFieldAndOrder() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        List<MarketEventTemplate> templates =
            new DefaultMarketEventTemplateCatalog().templates(now);

        assertTemplate(
            templates.get(0),
            "farming_bumper_crop",
            MarketEventScope.CATEGORY,
            80,
            true,
            false,
            1_800L,
            3_600L,
            9_200,
            9_700,
            "DOWN",
            7_200L,
            "Bumper Crop",
            "Farms are overflowing, softening prices for a while.",
            "Farming goods",
            "{\"categoryIds\":[\"farming\"]}",
            now
        );
        assertTemplate(
            templates.get(1),
            "mining_tool_shortage",
            MarketEventScope.CATEGORY,
            70,
            true,
            false,
            1_800L,
            3_600L,
            10_300,
            10_800,
            "UP",
            7_200L,
            "Tool Shortage",
            "Mining supplies are tight, lifting mineral prices.",
            "Mining goods",
            "{\"categoryIds\":[\"minerals\"]}",
            now
        );
        assertTemplate(
            templates.get(2),
            "rare_customs_hold",
            MarketEventScope.ITEM,
            0,
            false,
            true,
            900L,
            1_800L,
            10_000,
            10_000,
            "BLOCK",
            21_600L,
            "Customs Hold",
            "A specific good is temporarily held from trade.",
            "One item",
            "{\"manualOnly\":true}",
            now
        );
        assertTemplate(
            templates.get(3),
            "extra_rare_market_alarm",
            MarketEventScope.MARKET_WIDE,
            0,
            false,
            false,
            600L,
            1_200L,
            11_000,
            11_500,
            "UP",
            86_400L,
            "Market Alarm",
            "Market reviews are slowing supply, lifting prices across the board.",
            "World market",
            "{\"manualOnly\":true,\"automaticDisabled\":true}",
            now
        );
        assertEquals(4, templates.size());
    }

    private void assertTemplate(
        MarketEventTemplate template,
        String templateId,
        MarketEventScope scope,
        int automaticWeight,
        boolean automaticEnabled,
        boolean blockingAllowed,
        long minDurationSeconds,
        long maxDurationSeconds,
        int minEffectBasisPoints,
        int maxEffectBasisPoints,
        String effectDirection,
        long cooldownSeconds,
        String playerFacingName,
        String playerFacingDescription,
        String broadScopeHint,
        String eligibleTargetMetadata,
        Instant timestamp
    ) {
        assertEquals(templateId, template.getTemplateId());
        assertEquals(scope, template.getScope());
        assertEquals(automaticWeight, template.getAutomaticWeight());
        assertEquals(automaticEnabled, template.isAutomaticEnabled());
        assertEquals(blockingAllowed, template.isBlockingAllowed());
        assertEquals(minDurationSeconds, template.getMinDurationSeconds());
        assertEquals(maxDurationSeconds, template.getMaxDurationSeconds());
        assertEquals(minEffectBasisPoints, template.getMinEffectBasisPoints());
        assertEquals(maxEffectBasisPoints, template.getMaxEffectBasisPoints());
        assertEquals(effectDirection, template.getEffectDirection());
        assertEquals(cooldownSeconds, template.getCooldownSeconds());
        assertEquals(playerFacingName, template.getPlayerFacingName());
        assertEquals(
            playerFacingDescription,
            template.getPlayerFacingDescription()
        );
        assertEquals(broadScopeHint, template.getBroadScopeHint());
        assertEquals(eligibleTargetMetadata, template.getEligibleTargetMetadata());
        assertEquals(timestamp, template.getCreatedAt());
        assertEquals(timestamp, template.getUpdatedAt());
    }

    private MarketEventTemplate template(String templateId, Instant timestamp) {
        return new MarketEventTemplateBuilder()
            .templateId(templateId)
            .scope(MarketEventScope.MARKET_WIDE)
            .automaticWeight(25)
            .automaticEnabled(true)
            .blockingAllowed(false)
            .minDurationSeconds(1_800L)
            .maxDurationSeconds(3_600L)
            .minEffectBasisPoints(10_200)
            .maxEffectBasisPoints(10_500)
            .effectDirection("UP")
            .cooldownSeconds(7_200L)
            .playerFacingName("Crafting Festival")
            .playerFacingDescription(
                "Demand is lifting prices across the market."
            )
            .broadScopeHint("World market")
            .eligibleTargetMetadata("{}")
            .timestamps(timestamp)
            .build();
    }

    private MarketEventTemplateUpdateRequestDTO validUpdateRequest() {
        return new MarketEventTemplateUpdateRequestDTO(
            MarketEventScope.CATEGORY,
            15,
            true,
            false,
            1_800L,
            5_400L,
            9_400,
            9_800,
            10_800L,
            "Quiet Market",
            "Supply is softening category prices.",
            "Farming goods",
            "{\"categoryIds\":[\"farming\"]}"
        );
    }

    private MarketEventTemplateUpdateRequestDTO invalidDurationUpdateRequest() {
        return new MarketEventTemplateUpdateRequestDTO(
            MarketEventScope.CATEGORY,
            15,
            true,
            false,
            1_800L,
            1_200L,
            9_400,
            9_800,
            10_800L,
            "Quiet Market",
            "Supply is softening category prices.",
            "Farming goods",
            "{\"categoryIds\":[\"farming\"]}"
        );
    }

    private MarketEventTemplateUpdateRequestDTO validBlockUpdateRequest() {
        return new MarketEventTemplateUpdateRequestDTO(
            MarketEventScope.ITEM,
            0,
            false,
            true,
            900L,
            1_800L,
            10_000,
            10_000,
            21_600L,
            "Customs Hold",
            "A specific good is temporarily held from trade.",
            "One item",
            "{\"manualOnly\":true}"
        );
    }

    private MarketEventTemplateUpdateRequestDTO mixedEffectRangeUpdateRequest() {
        return new MarketEventTemplateUpdateRequestDTO(
            MarketEventScope.CATEGORY,
            15,
            true,
            false,
            1_800L,
            5_400L,
            9_800,
            10_200,
            10_800L,
            "Mixed Market",
            "A contradictory market signal crosses neutral pricing.",
            "Farming goods",
            "{\"categoryIds\":[\"farming\"]}"
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
