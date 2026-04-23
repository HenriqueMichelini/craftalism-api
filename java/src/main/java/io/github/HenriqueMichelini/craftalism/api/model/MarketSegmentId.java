package io.github.HenriqueMichelini.craftalism.api.model;

import java.io.Serializable;
import java.util.Objects;

public class MarketSegmentId implements Serializable {

    private String itemId;
    private long segmentIndex;

    public MarketSegmentId() {}

    public MarketSegmentId(String itemId, long segmentIndex) {
        this.itemId = itemId;
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
