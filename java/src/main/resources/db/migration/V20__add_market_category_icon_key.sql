ALTER TABLE market_categories
    ADD COLUMN icon_key VARCHAR(64) NOT NULL DEFAULT 'CHEST';

UPDATE market_categories
SET icon_key = CASE category_id
    WHEN 'farming' THEN 'WHEAT'
    WHEN 'animal_products' THEN 'BEEF'
    WHEN 'mob_drops' THEN 'BONE'
    WHEN 'natural_blocks' THEN 'DIRT'
    WHEN 'decorative_blocks' THEN 'WHITE_WOOL'
    WHEN 'forestry' THEN 'OAK_LOG'
    WHEN 'minerals' THEN 'COAL'
    ELSE icon_key
END;
