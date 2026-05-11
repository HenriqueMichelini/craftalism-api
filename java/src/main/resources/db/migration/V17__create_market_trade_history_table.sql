CREATE TABLE market_trade_history (
    id BIGSERIAL PRIMARY KEY,
    player_uuid UUID NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL,
    unit_price BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    currency VARCHAR(32) NOT NULL,
    snapshot_version VARCHAR(128) NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CHECK (quantity > 0),
    CHECK (unit_price > 0),
    CHECK (total_price > 0)
);

CREATE INDEX idx_market_trade_history_player_uuid
    ON market_trade_history (player_uuid);

CREATE INDEX idx_market_trade_history_item_id
    ON market_trade_history (item_id);

CREATE INDEX idx_market_trade_history_side
    ON market_trade_history (side);

CREATE INDEX idx_market_trade_history_executed_at
    ON market_trade_history (executed_at DESC);

CREATE INDEX idx_market_trade_history_default_order
    ON market_trade_history (executed_at DESC, id DESC);
