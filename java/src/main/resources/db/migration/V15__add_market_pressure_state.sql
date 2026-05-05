CREATE TABLE market_pressure_legacy_consistency_check (
    inconsistent_count BIGINT NOT NULL CHECK (inconsistent_count = 0)
);

INSERT INTO market_pressure_legacy_consistency_check (inconsistent_count)
SELECT COUNT(*)
FROM market_items item
LEFT JOIN (
    SELECT
        item_id,
        SUM(remaining_capacity) AS remaining_stock
    FROM market_segments
    GROUP BY item_id
) segment_stock ON segment_stock.item_id = item.item_id
WHERE item.current_stock <> COALESCE(segment_stock.remaining_stock, 0);

DROP TABLE market_pressure_legacy_consistency_check;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS base_unit_price BIGINT NOT NULL DEFAULT 1;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS min_unit_price BIGINT NOT NULL DEFAULT 1;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS max_unit_price BIGINT NOT NULL DEFAULT 1;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS segment_size BIGINT NOT NULL DEFAULT 50;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS price_sensitivity NUMERIC(8, 4) NOT NULL DEFAULT 0.0800;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS base_regen_quantity BIGINT NOT NULL DEFAULT 1;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS regen_interval_seconds BIGINT NOT NULL DEFAULT 60;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS net_position BIGINT NOT NULL DEFAULT 0;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS min_net_position BIGINT NULL;

ALTER TABLE market_items
    ADD COLUMN IF NOT EXISTS max_net_position BIGINT NULL;

UPDATE market_items
SET
    base_unit_price = COALESCE(
        (
            SELECT MIN(segment.unit_price)
            FROM market_segments segment
            WHERE
                segment.item_id = market_items.item_id
                AND segment.segment_index = 0
        ),
        (
            SELECT MIN(segment.unit_price)
            FROM market_segments segment
            WHERE segment.item_id = market_items.item_id
        ),
        GREATEST(1, sell_unit_estimate)
    ),
    segment_size = 50,
    price_sensitivity = 0.0800,
    base_regen_quantity = 1,
    regen_interval_seconds = 60,
    net_position = COALESCE(
        (
            SELECT SUM(segment.max_capacity - segment.remaining_capacity)
            FROM market_segments segment
            WHERE segment.item_id = market_items.item_id
        ),
        0
    ),
    min_net_position = NULL,
    max_net_position = NULL;

UPDATE market_items
SET
    min_unit_price = GREATEST(
        1,
        CAST(ROUND(base_unit_price * 0.50) AS BIGINT)
    ),
    max_unit_price = CAST(ROUND(base_unit_price * 3.00) AS BIGINT);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_base_unit_price_positive
        CHECK (base_unit_price > 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_min_unit_price_positive
        CHECK (min_unit_price > 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_min_unit_price_lte_base
        CHECK (min_unit_price <= base_unit_price);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_max_unit_price_gte_base
        CHECK (max_unit_price >= base_unit_price);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_segment_size_positive
        CHECK (segment_size > 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_price_sensitivity_positive
        CHECK (price_sensitivity > 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_base_regen_quantity_non_negative
        CHECK (base_regen_quantity >= 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_regen_interval_seconds_positive
        CHECK (regen_interval_seconds > 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_min_net_position_non_positive
        CHECK (min_net_position IS NULL OR min_net_position <= 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_max_net_position_non_negative
        CHECK (max_net_position IS NULL OR max_net_position >= 0);

ALTER TABLE market_items
    ADD CONSTRAINT chk_market_items_net_position_bounds_ordered
        CHECK (
            min_net_position IS NULL
            OR max_net_position IS NULL
            OR min_net_position <= max_net_position
        );
