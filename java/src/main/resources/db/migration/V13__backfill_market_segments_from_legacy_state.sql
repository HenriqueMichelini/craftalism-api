INSERT INTO market_segments (item_id, segment_index, max_capacity, remaining_capacity, unit_price)
WITH RECURSIVE legacy_items(item_id, current_stock, consumed_quantity, segment_count, base_unit_price, total_capacity) AS (
    SELECT
        item_id,
        current_stock,
        CASE
            WHEN market_momentum < 0 THEN 0
            ELSE market_momentum
        END AS consumed_quantity,
        CAST(
            CEIL(
                GREATEST(
                    current_stock + CASE
                        WHEN market_momentum < 0 THEN 0
                        ELSE market_momentum
                    END,
                    1
                ) / 50.0
            ) AS BIGINT
        ) AS segment_count,
        GREATEST(
            1,
            buy_unit_estimate - CAST(
                FLOOR(
                    CASE
                        WHEN market_momentum < 0 THEN 0
                        ELSE market_momentum
                    END / 50.0
                ) AS BIGINT
            )
        ) AS base_unit_price,
        GREATEST(
            current_stock + CASE
                WHEN market_momentum < 0 THEN 0
                ELSE market_momentum
            END,
            1
        ) AS total_capacity
    FROM market_items
),
expanded(item_id, segment_index, consumed_quantity, segment_count, base_unit_price, total_capacity) AS (
    SELECT
        item_id,
        0 AS segment_index,
        consumed_quantity,
        segment_count,
        base_unit_price,
        total_capacity
    FROM legacy_items
    UNION ALL
    SELECT
        item_id,
        segment_index + 1,
        consumed_quantity,
        segment_count,
        base_unit_price,
        total_capacity
    FROM expanded
    WHERE segment_index + 1 < segment_count
)
SELECT
    item_id,
    segment_index,
    max_capacity,
    max_capacity - LEAST(max_capacity, GREATEST(consumed_quantity - (segment_index * 50), 0)) AS remaining_capacity,
    base_unit_price + segment_index AS unit_price
FROM (
    SELECT
        item_id,
        segment_index,
        consumed_quantity,
        base_unit_price,
        LEAST(50, GREATEST(total_capacity - (segment_index * 50), 1)) AS max_capacity
    FROM expanded
) segments
WHERE NOT EXISTS (
    SELECT 1
    FROM market_segments existing
    WHERE existing.item_id = segments.item_id
);
