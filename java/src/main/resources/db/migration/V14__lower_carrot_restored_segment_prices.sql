UPDATE market_segments
SET unit_price = 1 + segment_index
WHERE item_id = 'carrot';
