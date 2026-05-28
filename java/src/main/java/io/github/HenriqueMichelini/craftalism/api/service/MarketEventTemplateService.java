package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketEventTemplateService {

    private final MarketEventTemplateRepository templateRepository;

    public MarketEventTemplateService(
        MarketEventTemplateRepository templateRepository
    ) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public void seedInitialTemplatesIfEmpty() {
        if (templateRepository.count() > 0L) {
            return;
        }
        templateRepository.saveAll(initialTemplates(Instant.now()));
    }

    List<MarketEventTemplate> initialTemplates(Instant now) {
        return List.of(
            template(
                "farming_bumper_crop",
                MarketEventRarity.MEDIUM,
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
            ),
            template(
                "mining_tool_shortage",
                MarketEventRarity.MEDIUM,
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
            ),
            template(
                "rare_customs_hold",
                MarketEventRarity.RARE,
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
            ),
            template(
                "extra_rare_market_alarm",
                MarketEventRarity.EXTRA_RARE,
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
            )
        );
    }

    private MarketEventTemplate template(
        String templateId,
        MarketEventRarity rarity,
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
        Instant now
    ) {
        MarketEventTemplate template = new MarketEventTemplate();
        template.setTemplateId(templateId);
        template.setRarity(rarity);
        template.setScope(scope);
        template.setAutomaticWeight(automaticWeight);
        template.setAutomaticEnabled(automaticEnabled);
        template.setBlockingAllowed(blockingAllowed);
        template.setMinDurationSeconds(minDurationSeconds);
        template.setMaxDurationSeconds(maxDurationSeconds);
        template.setMinEffectBasisPoints(minEffectBasisPoints);
        template.setMaxEffectBasisPoints(maxEffectBasisPoints);
        template.setEffectDirection(effectDirection);
        template.setCooldownSeconds(cooldownSeconds);
        template.setPlayerFacingName(playerFacingName);
        template.setPlayerFacingDescription(playerFacingDescription);
        template.setBroadScopeHint(broadScopeHint);
        template.setEligibleTargetMetadata(eligibleTargetMetadata);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        return template;
    }
}
