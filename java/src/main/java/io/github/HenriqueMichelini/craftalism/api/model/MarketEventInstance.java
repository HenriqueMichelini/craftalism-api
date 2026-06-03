package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity(name = "market_event_instances")
@Table(name = "market_event_instances")
public class MarketEventInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 96)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketEventSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketEventScope scope;

    @Column
    private String selectedCategoryId;

    @Column(length = 4096)
    private String selectedItemIds;

    @Column(nullable = false)
    private int effectBasisPoints;

    @Column(nullable = false)
    private int effectVersion = 1;

    @Column(nullable = false)
    private boolean blocking;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MarketEventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MarketEventEndReason endReason;

    @Column(length = 64)
    private String activeSlot;

    @Column(length = 128)
    private String actor;

    @Column(length = 4096)
    private String auditMetadata;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public MarketEventSource getSource() {
        return source;
    }

    public void setSource(MarketEventSource source) {
        this.source = source;
    }

    public MarketEventScope getScope() {
        return scope;
    }

    public void setScope(MarketEventScope scope) {
        this.scope = scope;
    }

    public String getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public String getSelectedItemIds() {
        return selectedItemIds;
    }

    public void setSelectedItemIds(String selectedItemIds) {
        this.selectedItemIds = selectedItemIds;
    }

    public int getEffectBasisPoints() {
        return effectBasisPoints;
    }

    public void setEffectBasisPoints(int effectBasisPoints) {
        this.effectBasisPoints = effectBasisPoints;
    }

    public int getEffectVersion() {
        return effectVersion;
    }

    public void setEffectVersion(int effectVersion) {
        this.effectVersion = effectVersion;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public MarketEventStatus getStatus() {
        return status;
    }

    public void setStatus(MarketEventStatus status) {
        this.status = status;
        this.activeSlot =
            status == MarketEventStatus.ACTIVE ? "GLOBAL" : null;
    }

    public MarketEventEndReason getEndReason() {
        return endReason;
    }

    public void setEndReason(MarketEventEndReason endReason) {
        this.endReason = endReason;
    }

    public String getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(String activeSlot) {
        this.activeSlot = activeSlot;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAuditMetadata() {
        return auditMetadata;
    }

    public void setAuditMetadata(String auditMetadata) {
        this.auditMetadata = auditMetadata;
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
