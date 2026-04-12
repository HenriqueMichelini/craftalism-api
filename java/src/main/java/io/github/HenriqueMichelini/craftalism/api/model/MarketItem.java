package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity(name = "market_items")
@Table(name = "market_items")
public class MarketItem {

    @Id
    @Column(nullable = false, updatable = false)
    private String itemId;

    @Column(nullable = false)
    private String categoryId;

    @Column(nullable = false)
    private String categoryDisplayName;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String iconKey;

    @Column(nullable = false)
    private long buyUnitEstimate;

    @Column(nullable = false)
    private long sellUnitEstimate;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private long currentStock;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal variationPercent;

    @Column(nullable = false)
    private boolean blocked;

    @Column(nullable = false)
    private boolean operating;

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryDisplayName() {
        return categoryDisplayName;
    }

    public void setCategoryDisplayName(String categoryDisplayName) {
        this.categoryDisplayName = categoryDisplayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public long getBuyUnitEstimate() {
        return buyUnitEstimate;
    }

    public void setBuyUnitEstimate(long buyUnitEstimate) {
        this.buyUnitEstimate = buyUnitEstimate;
    }

    public long getSellUnitEstimate() {
        return sellUnitEstimate;
    }

    public void setSellUnitEstimate(long sellUnitEstimate) {
        this.sellUnitEstimate = sellUnitEstimate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(long currentStock) {
        this.currentStock = currentStock;
    }

    public BigDecimal getVariationPercent() {
        return variationPercent;
    }

    public void setVariationPercent(BigDecimal variationPercent) {
        this.variationPercent = variationPercent;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isOperating() {
        return operating;
    }

    public void setOperating(boolean operating) {
        this.operating = operating;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
