package io.github.HenriqueMichelini.craftalism.api.model;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "market_quotes")
@Table(name = "market_quotes")
public class MarketQuote {

    public enum Status {
        ACTIVE,
        CONSUMED,
        EXPIRED,
        INVALIDATED,
    }

    @Id
    @Column(nullable = false, updatable = false)
    private String quoteToken;

    @Column(nullable = false)
    private UUID playerUuid;

    @Column(nullable = false)
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private MarketSide side;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private long totalPrice;

    @Column(nullable = false)
    private String snapshotVersion;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column
    private Instant resolvedAt;

    public String getQuoteToken() {
        return quoteToken;
    }

    public void setQuoteToken(String quoteToken) {
        this.quoteToken = quoteToken;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public MarketSide getSide() {
        return side;
    }

    public void setSide(MarketSide side) {
        this.side = side;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(String snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
