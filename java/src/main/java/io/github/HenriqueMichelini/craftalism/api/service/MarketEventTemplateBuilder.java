package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import java.time.Instant;

final class MarketEventTemplateBuilder {

    private final MarketEventTemplate template = new MarketEventTemplate();

    MarketEventTemplateBuilder templateId(String templateId) {
        template.setTemplateId(templateId);
        return this;
    }

    MarketEventTemplateBuilder rarity(MarketEventRarity rarity) {
        template.setRarity(rarity);
        return this;
    }

    MarketEventTemplateBuilder scope(MarketEventScope scope) {
        template.setScope(scope);
        return this;
    }

    MarketEventTemplateBuilder automaticWeight(int automaticWeight) {
        template.setAutomaticWeight(automaticWeight);
        return this;
    }

    MarketEventTemplateBuilder automaticEnabled(boolean automaticEnabled) {
        template.setAutomaticEnabled(automaticEnabled);
        return this;
    }

    MarketEventTemplateBuilder blockingAllowed(boolean blockingAllowed) {
        template.setBlockingAllowed(blockingAllowed);
        return this;
    }

    MarketEventTemplateBuilder minDurationSeconds(long minDurationSeconds) {
        template.setMinDurationSeconds(minDurationSeconds);
        return this;
    }

    MarketEventTemplateBuilder maxDurationSeconds(long maxDurationSeconds) {
        template.setMaxDurationSeconds(maxDurationSeconds);
        return this;
    }

    MarketEventTemplateBuilder minEffectBasisPoints(int minEffectBasisPoints) {
        template.setMinEffectBasisPoints(minEffectBasisPoints);
        return this;
    }

    MarketEventTemplateBuilder maxEffectBasisPoints(int maxEffectBasisPoints) {
        template.setMaxEffectBasisPoints(maxEffectBasisPoints);
        return this;
    }

    MarketEventTemplateBuilder effectDirection(String effectDirection) {
        template.setEffectDirection(effectDirection);
        return this;
    }

    MarketEventTemplateBuilder cooldownSeconds(long cooldownSeconds) {
        template.setCooldownSeconds(cooldownSeconds);
        return this;
    }

    MarketEventTemplateBuilder playerFacingName(String playerFacingName) {
        template.setPlayerFacingName(playerFacingName);
        return this;
    }

    MarketEventTemplateBuilder playerFacingDescription(
        String playerFacingDescription
    ) {
        template.setPlayerFacingDescription(playerFacingDescription);
        return this;
    }

    MarketEventTemplateBuilder broadScopeHint(String broadScopeHint) {
        template.setBroadScopeHint(broadScopeHint);
        return this;
    }

    MarketEventTemplateBuilder eligibleTargetMetadata(
        String eligibleTargetMetadata
    ) {
        template.setEligibleTargetMetadata(eligibleTargetMetadata);
        return this;
    }

    MarketEventTemplateBuilder timestamps(Instant timestamp) {
        template.setCreatedAt(timestamp);
        template.setUpdatedAt(timestamp);
        return this;
    }

    MarketEventTemplate build() {
        return template;
    }
}
