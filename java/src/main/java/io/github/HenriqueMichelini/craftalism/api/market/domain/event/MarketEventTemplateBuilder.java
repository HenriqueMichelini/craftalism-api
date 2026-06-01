package io.github.HenriqueMichelini.craftalism.api.market.domain.event;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import java.time.Instant;

public final class MarketEventTemplateBuilder {

    private final MarketEventTemplate template = new MarketEventTemplate();

    public MarketEventTemplateBuilder templateId(String templateId) {
        template.setTemplateId(templateId);
        return this;
    }

    public MarketEventTemplateBuilder rarity(MarketEventRarity rarity) {
        template.setRarity(rarity);
        return this;
    }

    public MarketEventTemplateBuilder scope(MarketEventScope scope) {
        template.setScope(scope);
        return this;
    }

    public MarketEventTemplateBuilder automaticWeight(int automaticWeight) {
        template.setAutomaticWeight(automaticWeight);
        return this;
    }

    public MarketEventTemplateBuilder automaticEnabled(boolean automaticEnabled) {
        template.setAutomaticEnabled(automaticEnabled);
        return this;
    }

    public MarketEventTemplateBuilder blockingAllowed(boolean blockingAllowed) {
        template.setBlockingAllowed(blockingAllowed);
        return this;
    }

    public MarketEventTemplateBuilder minDurationSeconds(long minDurationSeconds) {
        template.setMinDurationSeconds(minDurationSeconds);
        return this;
    }

    public MarketEventTemplateBuilder maxDurationSeconds(long maxDurationSeconds) {
        template.setMaxDurationSeconds(maxDurationSeconds);
        return this;
    }

    public MarketEventTemplateBuilder minEffectBasisPoints(int minEffectBasisPoints) {
        template.setMinEffectBasisPoints(minEffectBasisPoints);
        return this;
    }

    public MarketEventTemplateBuilder maxEffectBasisPoints(int maxEffectBasisPoints) {
        template.setMaxEffectBasisPoints(maxEffectBasisPoints);
        return this;
    }

    public MarketEventTemplateBuilder effectDirection(String effectDirection) {
        template.setEffectDirection(effectDirection);
        return this;
    }

    public MarketEventTemplateBuilder cooldownSeconds(long cooldownSeconds) {
        template.setCooldownSeconds(cooldownSeconds);
        return this;
    }

    public MarketEventTemplateBuilder playerFacingName(String playerFacingName) {
        template.setPlayerFacingName(playerFacingName);
        return this;
    }

    public MarketEventTemplateBuilder playerFacingDescription(
        String playerFacingDescription
    ) {
        template.setPlayerFacingDescription(playerFacingDescription);
        return this;
    }

    public MarketEventTemplateBuilder broadScopeHint(String broadScopeHint) {
        template.setBroadScopeHint(broadScopeHint);
        return this;
    }

    public MarketEventTemplateBuilder eligibleTargetMetadata(
        String eligibleTargetMetadata
    ) {
        template.setEligibleTargetMetadata(eligibleTargetMetadata);
        return this;
    }

    public MarketEventTemplateBuilder timestamps(Instant timestamp) {
        template.setCreatedAt(timestamp);
        template.setUpdatedAt(timestamp);
        return this;
    }

    public MarketEventTemplate build() {
        return template;
    }
}
