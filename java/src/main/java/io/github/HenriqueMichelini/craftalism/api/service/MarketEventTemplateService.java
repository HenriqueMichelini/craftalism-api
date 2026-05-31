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
            new MarketEventTemplateBuilder()
                .templateId("farming_bumper_crop")
                .rarity(MarketEventRarity.MEDIUM)
                .scope(MarketEventScope.CATEGORY)
                .automaticWeight(80)
                .automaticEnabled(true)
                .blockingAllowed(false)
                .minDurationSeconds(1_800L)
                .maxDurationSeconds(3_600L)
                .minEffectBasisPoints(9_200)
                .maxEffectBasisPoints(9_700)
                .effectDirection("DOWN")
                .cooldownSeconds(7_200L)
                .playerFacingName("Bumper Crop")
                .playerFacingDescription(
                    "Farms are overflowing, softening prices for a while."
                )
                .broadScopeHint("Farming goods")
                .eligibleTargetMetadata("{\"categoryIds\":[\"farming\"]}")
                .timestamps(now)
                .build(),
            new MarketEventTemplateBuilder()
                .templateId("mining_tool_shortage")
                .rarity(MarketEventRarity.MEDIUM)
                .scope(MarketEventScope.CATEGORY)
                .automaticWeight(70)
                .automaticEnabled(true)
                .blockingAllowed(false)
                .minDurationSeconds(1_800L)
                .maxDurationSeconds(3_600L)
                .minEffectBasisPoints(10_300)
                .maxEffectBasisPoints(10_800)
                .effectDirection("UP")
                .cooldownSeconds(7_200L)
                .playerFacingName("Tool Shortage")
                .playerFacingDescription(
                    "Mining supplies are tight, lifting mineral prices."
                )
                .broadScopeHint("Mining goods")
                .eligibleTargetMetadata("{\"categoryIds\":[\"minerals\"]}")
                .timestamps(now)
                .build(),
            new MarketEventTemplateBuilder()
                .templateId("rare_customs_hold")
                .rarity(MarketEventRarity.RARE)
                .scope(MarketEventScope.ITEM)
                .automaticWeight(0)
                .automaticEnabled(false)
                .blockingAllowed(true)
                .minDurationSeconds(900L)
                .maxDurationSeconds(1_800L)
                .minEffectBasisPoints(10_000)
                .maxEffectBasisPoints(10_000)
                .effectDirection("BLOCK")
                .cooldownSeconds(21_600L)
                .playerFacingName("Customs Hold")
                .playerFacingDescription(
                    "A specific good is temporarily held from trade."
                )
                .broadScopeHint("One item")
                .eligibleTargetMetadata("{\"manualOnly\":true}")
                .timestamps(now)
                .build(),
            new MarketEventTemplateBuilder()
                .templateId("extra_rare_market_alarm")
                .rarity(MarketEventRarity.EXTRA_RARE)
                .scope(MarketEventScope.MARKET_WIDE)
                .automaticWeight(0)
                .automaticEnabled(false)
                .blockingAllowed(false)
                .minDurationSeconds(600L)
                .maxDurationSeconds(1_200L)
                .minEffectBasisPoints(11_000)
                .maxEffectBasisPoints(11_500)
                .effectDirection("UP")
                .cooldownSeconds(86_400L)
                .playerFacingName("Market Alarm")
                .playerFacingDescription(
                    "Market reviews are slowing supply, lifting prices across the board."
                )
                .broadScopeHint("World market")
                .eligibleTargetMetadata(
                    "{\"manualOnly\":true,\"automaticDisabled\":true}"
                )
                .timestamps(now)
                .build()
        );
    }
}
