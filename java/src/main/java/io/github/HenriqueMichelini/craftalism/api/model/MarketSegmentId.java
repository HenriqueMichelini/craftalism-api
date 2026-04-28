package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MarketSegmentId implements Serializable {

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "segment_index", nullable = false)
    private long segmentIndex;

    public MarketSegmentId() {}

    public MarketSegmentId(String itemId, long segmentIndex) {
        this.itemId = itemId;
        this.segmentIndex = segmentIndex;
    }

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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MarketSegmentId that)) {
            return false;
        }
        return segmentIndex == that.segmentIndex && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, segmentIndex);
    }
}
