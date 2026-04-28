package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity(name = "market_segments")
@Table(name = "market_segments")
public class MarketSegment {

    @EmbeddedId
    private MarketSegmentId id = new MarketSegmentId();

    @MapsId("itemId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private MarketItem item;

    @Column(nullable = false)
    private long maxCapacity;

    @Column(nullable = false)
    private long remainingCapacity;

    @Column(nullable = false)
    private long unitPrice;

    public MarketSegmentId getId() {
        return id;
    }

    public void setId(MarketSegmentId id) {
        this.id = id == null ? new MarketSegmentId() : id;
    }

    public String getItemId() {
        return id.getItemId();
    }

    public void setItemId(String itemId) {
        id.setItemId(itemId);
    }

    public long getSegmentIndex() {
        return id.getSegmentIndex();
    }

    public void setSegmentIndex(long segmentIndex) {
        id.setSegmentIndex(segmentIndex);
    }

    public MarketItem getItem() {
        return item;
    }

    public void setItem(MarketItem item) {
        this.item = item;
        id.setItemId(item == null ? null : item.getItemId());
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
