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
    market_momentum BIGINT NOT NULL DEFAULT 0,
    base_unit_price BIGINT NOT NULL DEFAULT 1,
    min_unit_price BIGINT NOT NULL DEFAULT 1,
    max_unit_price BIGINT NOT NULL DEFAULT 1,
    segment_size BIGINT NOT NULL DEFAULT 50,
    price_sensitivity NUMERIC(8, 4) NOT NULL DEFAULT 0.0800,
    sell_price_percentage NUMERIC(5, 4) NOT NULL DEFAULT 0.7000,
    base_regen_quantity BIGINT NOT NULL DEFAULT 1,
    regen_interval_seconds BIGINT NOT NULL DEFAULT 60,
    net_position BIGINT NOT NULL DEFAULT 0,
    min_net_position BIGINT NULL,
    max_net_position BIGINT NULL,
    variation_percent NUMERIC(7, 2) NOT NULL,
    blocked BOOLEAN NOT NULL,
    operating BOOLEAN NOT NULL,
    last_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_market_items_base_unit_price_positive
        CHECK (base_unit_price > 0),
    CONSTRAINT chk_market_items_min_unit_price_positive
        CHECK (min_unit_price > 0),
    CONSTRAINT chk_market_items_min_unit_price_lte_base
        CHECK (min_unit_price <= base_unit_price),
    CONSTRAINT chk_market_items_max_unit_price_gte_base
        CHECK (max_unit_price >= base_unit_price),
    CONSTRAINT chk_market_items_segment_size_positive
        CHECK (segment_size > 0),
    CONSTRAINT chk_market_items_price_sensitivity_positive
        CHECK (price_sensitivity > 0),
    CONSTRAINT chk_market_items_sell_price_percentage_ratio
        CHECK (sell_price_percentage > 0 AND sell_price_percentage < 1),
    CONSTRAINT chk_market_items_base_regen_quantity_non_negative
        CHECK (base_regen_quantity >= 0),
    CONSTRAINT chk_market_items_regen_interval_seconds_positive
        CHECK (regen_interval_seconds > 0),
    CONSTRAINT chk_market_items_min_net_position_non_positive
        CHECK (min_net_position IS NULL OR min_net_position <= 0),
    CONSTRAINT chk_market_items_max_net_position_non_negative
        CHECK (max_net_position IS NULL OR max_net_position >= 0),
    CONSTRAINT chk_market_items_net_position_bounds_ordered
        CHECK (
            min_net_position IS NULL
            OR max_net_position IS NULL
            OR min_net_position <= max_net_position
        )
);
