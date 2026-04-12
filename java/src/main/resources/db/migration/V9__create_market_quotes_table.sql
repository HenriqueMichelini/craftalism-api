CREATE TABLE market_quotes (
    quote_token VARCHAR(128) PRIMARY KEY,
    player_uuid UUID NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity BIGINT NOT NULL,
    unit_price BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    snapshot_version VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_market_quotes_expires_at ON market_quotes (expires_at);
CREATE INDEX idx_market_quotes_player_uuid ON market_quotes (player_uuid);
