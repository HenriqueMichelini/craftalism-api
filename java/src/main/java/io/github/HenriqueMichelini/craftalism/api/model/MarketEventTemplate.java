package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity(name = "market_event_templates")
@Table(name = "market_event_templates")
public class MarketEventTemplate {

    @Id
    @Column(nullable = false, updatable = false, length = 96)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketEventRarity rarity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketEventScope scope;

    @Column(nullable = false)
    private int automaticWeight;

    @Column(nullable = false)
    private boolean automaticEnabled;

    @Column(nullable = false)
    private boolean blockingAllowed;

    @Column(nullable = false)
    private long minDurationSeconds;

    @Column(nullable = false)
    private long maxDurationSeconds;

    @Column(nullable = false)
    private int minEffectBasisPoints;

    @Column(nullable = false)
    private int maxEffectBasisPoints;

    @Column(nullable = false, length = 16)
    private String effectDirection;

    @Column(nullable = false)
    private long cooldownSeconds;

    @Column(nullable = false)
    private String playerFacingName;

    @Column(nullable = false, length = 2048)
    private String playerFacingDescription;

    @Column(nullable = false)
    private String broadScopeHint;

    @Column(nullable = false, length = 4096)
    private String eligibleTargetMetadata;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public MarketEventRarity getRarity() {
        return rarity;
    }

    public void setRarity(MarketEventRarity rarity) {
        this.rarity = rarity;
    }

    public MarketEventScope getScope() {
        return scope;
    }

    public void setScope(MarketEventScope scope) {
        this.scope = scope;
    }

    public int getAutomaticWeight() {
        return automaticWeight;
    }

    public void setAutomaticWeight(int automaticWeight) {
        this.automaticWeight = automaticWeight;
    }

    public boolean isAutomaticEnabled() {
        return automaticEnabled;
    }

    public void setAutomaticEnabled(boolean automaticEnabled) {
        this.automaticEnabled = automaticEnabled;
    }

    public boolean isBlockingAllowed() {
        return blockingAllowed;
    }

    public void setBlockingAllowed(boolean blockingAllowed) {
        this.blockingAllowed = blockingAllowed;
    }

    public long getMinDurationSeconds() {
        return minDurationSeconds;
    }

    public void setMinDurationSeconds(long minDurationSeconds) {
        this.minDurationSeconds = minDurationSeconds;
    }

    public long getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public void setMaxDurationSeconds(long maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public int getMinEffectBasisPoints() {
        return minEffectBasisPoints;
    }

    public void setMinEffectBasisPoints(int minEffectBasisPoints) {
        this.minEffectBasisPoints = minEffectBasisPoints;
    }

    public int getMaxEffectBasisPoints() {
        return maxEffectBasisPoints;
    }

    public void setMaxEffectBasisPoints(int maxEffectBasisPoints) {
        this.maxEffectBasisPoints = maxEffectBasisPoints;
    }

    public String getEffectDirection() {
        return effectDirection;
    }

    public void setEffectDirection(String effectDirection) {
        this.effectDirection = effectDirection;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getPlayerFacingName() {
        return playerFacingName;
    }

    public void setPlayerFacingName(String playerFacingName) {
        this.playerFacingName = playerFacingName;
    }

    public String getPlayerFacingDescription() {
        return playerFacingDescription;
    }

    public void setPlayerFacingDescription(String playerFacingDescription) {
        this.playerFacingDescription = playerFacingDescription;
    }

    public String getBroadScopeHint() {
        return broadScopeHint;
    }

    public void setBroadScopeHint(String broadScopeHint) {
        this.broadScopeHint = broadScopeHint;
    }

    public String getEligibleTargetMetadata() {
        return eligibleTargetMetadata;
    }

    public void setEligibleTargetMetadata(String eligibleTargetMetadata) {
        this.eligibleTargetMetadata = eligibleTargetMetadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
