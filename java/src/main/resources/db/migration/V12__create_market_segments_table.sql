CREATE TABLE market_segments (
    item_id VARCHAR(64) NOT NULL,
    segment_index BIGINT NOT NULL,
    max_capacity BIGINT NOT NULL,
    remaining_capacity BIGINT NOT NULL,
    unit_price BIGINT NOT NULL,
    PRIMARY KEY (item_id, segment_index),
    CONSTRAINT fk_market_segments_item
        FOREIGN KEY (item_id) REFERENCES market_items (item_id) ON DELETE CASCADE
);

CREATE INDEX idx_market_segments_item_id ON market_segments (item_id);
