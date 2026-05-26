ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS sell_price_percentage NUMERIC(5, 4) NOT NULL DEFAULT 0.7000;

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_sell_price_percentage_ratio
        CHECK (sell_price_percentage > 0 AND sell_price_percentage < 1);
