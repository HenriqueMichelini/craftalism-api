CREATE TABLE market_items (
    item_id VARCHAR(64) PRIMARY KEY,
    category_id VARCHAR(64) NOT NULL,
    category_display_name VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    icon_key VARCHAR(64) NOT NULL,
    buy_unit_estimate BIGINT NOT NULL,
    sell_unit_estimate BIGINT NOT NULL,
    currency VARCHAR(32) NOT NULL,
    current_stock BIGINT NOT NULL,
    variation_percent NUMERIC(7, 2) NOT NULL,
    blocked BOOLEAN NOT NULL,
    operating BOOLEAN NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
