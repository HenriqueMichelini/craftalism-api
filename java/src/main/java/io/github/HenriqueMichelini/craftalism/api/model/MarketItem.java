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

    private static final BigDecimal DEFAULT_PRICE_SENSITIVITY = new BigDecimal(
        "0.0800"
    );
    private static final BigDecimal DEFAULT_SELL_PRICE_PERCENTAGE =
        new BigDecimal("0.7000");

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

    @Column(nullable = false)
    private long marketMomentum;

    @Column(name = "base_unit_price", nullable = false)
    private long baseUnitPrice = 1L;

    @Column(name = "min_unit_price", nullable = false)
    private long minUnitPrice = 1L;

    @Column(name = "max_unit_price", nullable = false)
    private long maxUnitPrice = 1L;

    @Column(name = "segment_size", nullable = false)
    private long segmentSize = 50L;

    @Column(name = "price_sensitivity", nullable = false, precision = 8, scale = 4)
    private BigDecimal priceSensitivity = DEFAULT_PRICE_SENSITIVITY;

    @Column(name = "sell_price_percentage", nullable = false, precision = 5, scale = 4)
    private BigDecimal sellPricePercentage = DEFAULT_SELL_PRICE_PERCENTAGE;

    @Column(name = "base_regen_quantity", nullable = false)
    private long baseRegenQuantity = 1L;

    @Column(name = "regen_interval_seconds", nullable = false)
    private long regenIntervalSeconds = 60L;

    @Column(name = "net_position", nullable = false)
    private long netPosition;

    @Column(name = "min_net_position")
    private Long minNetPosition;

    @Column(name = "max_net_position")
    private Long maxNetPosition;

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

    public long getMarketMomentum() {
        return marketMomentum;
    }

    public void setMarketMomentum(long marketMomentum) {
        this.marketMomentum = marketMomentum;
    }

    public long getBaseUnitPrice() {
        return baseUnitPrice;
    }

    public void setBaseUnitPrice(long baseUnitPrice) {
        this.baseUnitPrice = baseUnitPrice;
    }

    public long getMinUnitPrice() {
        return minUnitPrice;
    }

    public void setMinUnitPrice(long minUnitPrice) {
        this.minUnitPrice = minUnitPrice;
    }

    public long getMaxUnitPrice() {
        return maxUnitPrice;
    }

    public void setMaxUnitPrice(long maxUnitPrice) {
        this.maxUnitPrice = maxUnitPrice;
    }

    public long getSegmentSize() {
        return segmentSize;
    }

    public void setSegmentSize(long segmentSize) {
        this.segmentSize = segmentSize;
    }

    public BigDecimal getPriceSensitivity() {
        return priceSensitivity;
    }

    public void setPriceSensitivity(BigDecimal priceSensitivity) {
        this.priceSensitivity = priceSensitivity;
    }

    public BigDecimal getSellPricePercentage() {
        return sellPricePercentage;
    }

    public void setSellPricePercentage(BigDecimal sellPricePercentage) {
        this.sellPricePercentage = sellPricePercentage;
    }

    public long getBaseRegenQuantity() {
        return baseRegenQuantity;
    }

    public void setBaseRegenQuantity(long baseRegenQuantity) {
        this.baseRegenQuantity = baseRegenQuantity;
    }

    public long getRegenIntervalSeconds() {
        return regenIntervalSeconds;
    }

    public void setRegenIntervalSeconds(long regenIntervalSeconds) {
        this.regenIntervalSeconds = regenIntervalSeconds;
    }

    public long getNetPosition() {
        return netPosition;
    }

    public void setNetPosition(long netPosition) {
        this.netPosition = netPosition;
    }

    public Long getMinNetPosition() {
        return minNetPosition;
    }

    public void setMinNetPosition(Long minNetPosition) {
        this.minNetPosition = minNetPosition;
    }

    public Long getMaxNetPosition() {
        return maxNetPosition;
    }

    public void setMaxNetPosition(Long maxNetPosition) {
        this.maxNetPosition = maxNetPosition;
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
