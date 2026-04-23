package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity(name = "market_segments")
@Table(name = "market_segments")
@IdClass(MarketSegmentId.class)
public class MarketSegment {

    @Id
    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Id
    @Column(name = "segment_index", nullable = false)
    private long segmentIndex;

    @Column(nullable = false)
    private long maxCapacity;

    @Column(nullable = false)
    private long remainingCapacity;

    @Column(nullable = false)
    private long unitPrice;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public long getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(long segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    public long getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(long remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(long unitPrice) {
        this.unitPrice = unitPrice;
    }
}
