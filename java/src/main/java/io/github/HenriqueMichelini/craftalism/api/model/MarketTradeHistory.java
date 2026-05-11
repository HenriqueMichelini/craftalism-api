package io.github.HenriqueMichelini.craftalism.api.model;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "market_trade_history")
@Table(name = "market_trade_history")
public class MarketTradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private UUID playerUuid;

    @Column(nullable = false, updatable = false, length = 64)
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 8)
    private MarketSide side;

    @Column(nullable = false, updatable = false)
    private long quantity;

    @Column(nullable = false, updatable = false)
    private long unitPrice;

    @Column(nullable = false, updatable = false)
    private long totalPrice;

    @Column(nullable = false, updatable = false, length = 32)
    private String currency;

    @Column(nullable = false, updatable = false, length = 128)
    private String snapshotVersion;

    @Column(nullable = false, updatable = false)
    private Instant executedAt;

    public Long getId() {
        return id;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(String snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
